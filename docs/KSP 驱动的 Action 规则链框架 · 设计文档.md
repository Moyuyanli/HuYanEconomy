# KSP 驱动的 Action 规则链框架 · 设计文档

> 版本：v2.0（最终版）
> 适用：基于 mirai 的 Kotlin 插件
> 核心目标：为"多游戏动作 + 通用系统横切逻辑"提供**编译期安全、零反射、全协程、高扩展**的规则链框架

---

## 1. 背景与目标

### 1.1 问题

插件内有多个游戏动作（签到、钓鱼、抽卡……），每个动作前后需要挂载通用系统的横切逻辑（条件检查、称号加成、道具加成、消费、结算）。

现状痛点：

- **步骤挤在一起**：检查、消费、执行写在一个事件里，臃肿。
- **数据靠事件传递**：倍率等中间数据靠 mirai 事件字段传递，改一处动全身。
- **扩展难**：新增一个加成来源，要改事件链。
- **回滚缺失**：消费了道具但后续失败，没有自动补偿。
- **语义单一**：只能 `throw` 表达失败，无法区分"拦截/跳过/继续/回滚"。

### 1.2 目标

| 目标           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| **注解声明**   | 动作打 `@Action`，规则打 `@Before`/`@After`，类型打 `@Provides` |
| **职责分离**   | 动作作者只写业务，规则作者只写规则，装配维护者只注册类型     |
| **规则可插拔** | 新增系统只写规则，不动已有代码                               |
| **编译期安全** | actionId、参数类型、返回值类型全部编译期校验                 |
| **零反射**     | KSP 生成全部调用代码                                         |
| **全协程**     | 规则、动作、引擎全链路 `suspend`                             |
| **一行入口**   | `XxxActions.方法名(event)`                                   |
| **安全可控**   | `RuleResult` 表达业务结论，引擎自动逆序补偿                  |

### 1.3 非目标

- 不支持运行时动态注册规则。
- 不支持任意方法无感拦截。
- 第一版不支持 `@Named` 多实例同类型注入。

---

## 2. 总体设计

### 2.1 架构图

```
┌──────────────────────────────────────────────────────────┐
│  入口层（mirai 事件）                                     │
│  @MessageAuthorize → suspend fun sign(event)             │
│       └── SignUsecaseActions.sign(event)  ← 一行         │
└──────────────────────────┬───────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────┐
│  引擎层 ActionEngine.invokeFromEntry                     │
│  1. 提取 userId                                          │
│  2. 装配 ActionContext（按 KSP 反推的需求列表）          │
│  3. before 规则（按 order）→ 消费 RuleResult             │
│  4. 执行动作本体                                         │
│  5. after 规则（按 order）                               │
│  6. 失败 → 逆序补偿                                      │
└──────────────────────────┬───────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────┐
│  注册表层（KSP 生成）                                     │
│  ActionRegistry_Generated                                │
│  BeforeRegistry_Generated                                │
│  AfterRegistry_Generated                                 │
│  ActionContextRequirements_Generated                     │
│  XxxActions（每个业务类一个门面）                         │
└──────────────────────────┬───────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────┐
│  业务层                                                   │
│  @Action suspend fun sign(event): SignResult             │
│  @Before / @After 规则方法                                │
│  @Provides 类型加载声明                                   │
└──────────────────────────────────────────────────────────┘
```

### 2.2 核心概念

| 概念              | 说明                                               |
| ----------------- | -------------------------------------------------- |
| **Action**        | 可被规则链包裹的业务动作，`@Action(id)` 标记       |
| **Rule**          | 横切逻辑，`@Before`/`@After` 标记，绑定 actionId   |
| **Provider**      | 类型加载声明，`@Provides` 标记                     |
| **ActionContext** | 执行上下文，承载入参、实体、返回值、自定义数据     |
| **ActionEngine**  | 执行引擎，装配、排序、执行、补偿                   |
| **RuleResult**    | 规则结论密封类：`Continue`/`Skip`/`Abort`/`Reject` |
| **Compensation**  | 补偿器，规则失败时回滚副作用                       |
| **XxxActions**    | KSP 生成的门面，入口调用它即走规则链               |

### 2.3 三个角色

| 角色           | 写什么                  | 关注点                              |
| -------------- | ----------------------- | ----------------------------------- |
| **动作作者**   | `@Action` 方法          | 只写业务逻辑，不声明依赖            |
| **规则作者**   | `@Before`/`@After` 方法 | 只声明所需参数类型，不碰 ctx 取数据 |
| **装配维护者** | `@Provides` 方法        | 只声明"某类型怎么从 userId 加载"    |

**三者互不干扰，各自独立演进。**

---

## 3. 注解定义（4 个核心注解）

### 3.1 `@Action`

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Action(
    val id: String,
)
```

**动作作者只声明 id，不声明任何依赖。**

### 3.2 `@Before` / `@After`

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Before(
    val value: String,
    val order: Int = 100,
    val onFail: FailStrategy = FailStrategy.ABORT,
    val compensate: KClass<out Compensation>? = null,
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class After(
    val value: String,
    val order: Int = 100,
    val compensate: KClass<out Compensation>? = null,
)

enum class FailStrategy { ABORT, SKIP, IGNORE }
```

### 3.3 `@Provides`

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Provides
```

**装配维护者用它声明"某类型怎么加载"。**

### 3.4 `order` 约定

| 区间  | 用途                                     |
| ----- | ---------------------------------------- |
| 1–4   | 冷却、频率限制                           |
| 5–9   | 资格检查（已签到、称号、等级）           |
| 10–49 | 加成产出（称号倍率、道具倍率、福利倍率） |
| 50–99 | 资源扣除（消费道具、扣费）               |
| 100+  | 奖励发放、日志（after）                  |

**写进团队规范，顺序错乱会导致"扣了钱没执行"。**

---

## 4. 运行时基础设施

### 4.1 RuleResult（规则结论）

```kotlin
sealed interface RuleResult {

    /** 继续执行下一个规则（默认） */
    object Continue : RuleResult

    /** 跳过当前动作的后续分支，但不视为失败 */
    data class Skip(val reason: String) : RuleResult

    /** 阻止动作，中断并回滚 */
    data class Abort(
        val reason: String,
        val rollbackTo: RollbackScope = RollbackScope.ALL,
    ) : RuleResult

    /** 阻止动作，带结构化信息 */
    data class Reject(
        val code: String,
        val message: String,
        val rollbackTo: RollbackScope = RollbackScope.ALL,
    ) : RuleResult
}

enum class RollbackScope { ALL, CURRENT_ACTION, NONE }
```

**语义对照**：

| 结果       | 语义             | 引擎行为                                     |
| ---------- | ---------------- | -------------------------------------------- |
| `Continue` | 继续             | 记录，继续                                   |
| `Skip`     | 跳过分支，不失败 | 记录（有副作用仍补偿），继续                 |
| `Abort`    | 阻止，回滚       | 逆序补偿，抛 `ActionAborted`                 |
| `Reject`   | 阻止，带原因     | 逆序补偿，抛 `ActionRejected(code, message)` |

### 4.2 ActionContext

```kotlin
class ActionContext(
    val actionId: String,
    val userId: Long,
    val event: Any?,
) {
    private val pool = mutableMapOf<KClass<*>, Any>()
    private var result: Any? = null
    val attrs = mutableMapOf<String, Any?>()

    fun <T : Any> put(type: KClass<T>, value: T) { pool[type] = value }

    fun <T : Any> get(type: KClass<T>): T? {
        pool[type]?.let { return it as T }
        return pool.entries.firstOrNull { type.isInstance(it.value) }?.value as? T
    }

    fun setResult(r: Any?) { result = r }

    fun <T : Any> result(type: KClass<T>): T? {
        result?.let { if (type.isInstance(it)) return it as T }
        return null
    }
}
```

**参数来源对照表**：

| 来源                 | 谁 put             | 何时可用  | 规则怎么取                |
| -------------------- | ------------------ | --------- | ------------------------- |
| `ActionContext` 本体 | 引擎创建           | 全程      | 声明 `ActionContext` 参数 |
| 动作入参             | 入口/引擎          | Before 起 | `ctx.get(XxxClass)`       |
| 预装配实体           | 引擎（按需求列表） | Before 起 | `ctx.get(XxxClass)`       |
| 动作返回值           | 引擎自动           | After 起  | `ctx.result(XxxClass)`    |
| 自定义数据           | 规则/动作手动      | 任意      | `ctx.attrs["key"]`        |

### 4.3 倍率模型（可选，用于加成类场景）

```kotlin
sealed interface RewardModifier {
    val source: String

    data class Add(override val source: String, val value: Double) : RewardModifier
    data class Multiply(override val source: String, val factor: Double) : RewardModifier
}

fun List<RewardModifier>.applyTo(base: Double): Double {
    val addSum = filterIsInstance<RewardModifier.Add>().sumOf { it.value }
    val mulProduct = filterIsInstance<RewardModifier.Multiply>()
        .fold(1.0) { acc, m -> acc * m.factor }
    return (base + addSum) * mulProduct
}

// ctx 扩展
private const val KEY_MODIFIERS = "rewardModifiers"

fun ActionContext.addModifier(m: RewardModifier) {
    @Suppress("UNCHECKED_CAST")
    val list = (attrs[KEY_MODIFIERS] as? MutableList<RewardModifier>) ?: mutableListOf()
    list.add(m)
    attrs[KEY_MODIFIERS] = list
}

fun ActionContext.modifiers(): List<RewardModifier> =
    (attrs[KEY_MODIFIERS] as? List<RewardModifier>) ?: emptyList()
```

**倍率不"传递"，而是"累加进 ctx"，结算时统一计算。**

### 4.4 SuspendRule 与 ActionMeta

```kotlin
class SuspendRule(
    val order: Int,
    val compensate: (suspend (ActionContext, Throwable) -> Unit)? = null,
    val invoke: suspend (ActionContext) -> RuleResult,
)

class ActionMeta(
    val id: String,
    val invoke: suspend (ActionContext) -> Any?,
)
```

### 4.5 ContextRegistry（类型注册中心）

```kotlin
class ContextRegistry {
    private val loaders = mutableMapOf<KClass<*>, suspend (Long) -> Any>()

    fun <T : Any> register(type: KClass<T>, loader: suspend (Long) -> T) {
        loaders[type] = loader
    }

    suspend fun load(type: KClass<*>, userId: Long): Any {
        val loader = loaders[type] ?: error("未注册的上下文类型: $type")
        return loader(userId)
    }

    fun has(type: KClass<*>): Boolean = type in loaders
}
```

**由 `@Provides` 生成或手动注册。**

### 4.6 ActionEngine

```kotlin
class ActionEngine(
    private val registry: ContextRegistry,
    private val actionRegistry: Map<String, ActionMeta>,
    private val beforeRegistry: Map<String, List<SuspendRule>>,
    private val afterRegistry: Map<String, List<SuspendRule>>,
    private val requirements: Map<String, List<KClass<*>>>,
) {
    suspend fun <T> invokeFromEntry(
        actionId: String,
        event: Any,
        userId: Long = extractUserId(event),
        block: suspend (ActionContext) -> T,
    ): T {
        val meta = actionRegistry[actionId] ?: error("未注册的 action: $actionId")
        val ctx = ActionContext(actionId, userId, event)
        ctx.put(event::class, event)

        // 按 KSP 反推的需求列表装配
        requirements[actionId].orEmpty().forEach { type ->
            ctx.put(type, registry.load(type, userId))
        }

        val befores = beforeRegistry[actionId].orEmpty()
        val afters = afterRegistry[actionId].orEmpty()
        val executed = mutableListOf<SuspendRule>()

        try {
            // ---- 前置 ----
            for (rule in befores) {
                when (val r = rule.invoke(ctx)) {
                    is RuleResult.Continue -> executed += rule
                    is RuleResult.Skip -> executed += rule
                    is RuleResult.Abort -> {
                        rollback(executed, ctx, r.reason)
                        throw ActionAborted(r.reason)
                    }
                    is RuleResult.Reject -> {
                        rollback(executed, ctx, r.message)
                        throw ActionRejected(r.code, r.message)
                    }
                }
            }

            // ---- 动作本体 ----
            val result = block(ctx)
            ctx.setResult(result)

            // ---- 后置 ----
            for (rule in afters) {
                when (val r = rule.invoke(ctx)) {
                    is RuleResult.Continue, is RuleResult.Skip -> { /* ok */ }
                    is RuleResult.Abort -> log.error("after abort: ${r.reason}")
                    is RuleResult.Reject -> log.error("after reject: ${r.message}")
                }
            }

            return result

        } catch (e: Throwable) {
            if (executed.isNotEmpty()) rollback(executed, ctx, e.message ?: "unknown")
            throw e
        }
    }

    private suspend fun rollback(
        executed: List<SuspendRule>,
        ctx: ActionContext,
        reason: String,
    ) {
        executed.asReversed().forEach { rule ->
            rule.compensate?.let {
                runCatching { it(ctx, ActionAborted(reason)) }
                    .onFailure { ex -> log.error("补偿失败", ex) }
            }
        }
    }

    companion object {
        private fun extractUserId(event: Any): Long = when (event) {
            is GroupMessageEvent -> event.sender.id
            is FriendMessageEvent -> event.sender.id
            else -> error("无法从 $event 提取 userId")
        }
    }
}
```

**执行语义**：

- before 按 `order` 升序，任一步 `Abort`/`Reject` → 逆序补偿已执行规则。
- 动作本体返回后 `setResult`，after 才能取。
- 任意阶段抛异常 → 兜底逆序补偿。
- 补偿失败只记日志，不掩盖原始异常。

---

## 5. 规则编写规范

### 5.1 两种签名

**签名 A：返回 `Unit`（默认 `Continue`）**

```kotlin
@Before("daily.sign", order = 5)
suspend fun applyTitleBonus(userTitle: UserTitle, ctx: ActionContext) {
    titleService.rewardModifier(userTitle)?.let { ctx.addModifier(it) }
}
```

KSP 生成：

```kotlin
SuspendRule(order = 5) { ctx ->
    SignRules.applyTitleBonus(ctx.get(UserTitle::class)!!, ctx)
    RuleResult.Continue   // 自动补
}
```

**签名 B：返回 `RuleResult`（显式结论）**

```kotlin
@Before("daily.sign", order = 1)
suspend fun checkAlreadySigned(userInfo: UserInfoDto): RuleResult =
    if (userInfo.signedToday) RuleResult.Reject("ALREADY_SIGNED", "今日已签到")
    else RuleResult.Continue
```

**规则作者按需选择**：正常规则写 `Unit`，有结论写 `RuleResult`。

### 5.2 参数自动注入

规则方法的参数**由 KSP 自动从 ctx 取**，规则作者不碰 ctx：

| 参数类型                              | KSP 生成                 |
| ------------------------------------- | ------------------------ |
| `ActionContext`                       | 直接传 `ctx`             |
| `@Action` 的返回值类型（仅 `@After`） | `ctx.result(XxxClass)!!` |
| 已注册的上下文类型                    | `ctx.get(XxxClass)!!`    |
| 未注册类型                            | **编译报错**             |
| `@Before` 引用返回值类型              | **编译报错**             |

### 5.3 自定义数据用 `ctx.attrs`

规则特有的中间数据（如"消费了什么道具"）放 `ctx.attrs`，不进 `@Provides`：

```kotlin
ctx.attrs["consumed"] = item to 1
```

**建议用类型化扩展封装**：

```kotlin
var ActionContext.consumed: Pair<String, Int>?
    get() = attrs["consumed"] as? Pair<String, Int>
    set(v) { attrs["consumed"] = v }
```

### 5.4 补偿器

```kotlin
interface Compensation {
    suspend fun compensate(ctx: ActionContext, cause: Throwable)
}

class ConsumeCompensation(
    private val backpackService: BackpackService,
) : Compensation {
    override suspend fun compensate(ctx: ActionContext, cause: Throwable) {
        val consumed = ctx.consumed ?: return
        backpackService.add(ctx.userId, consumed.first, consumed.second)
    }
}
```

**编写原则**：
- 幂等：补偿可能被多次调用（引擎保证一次，但业务层应容错）。
- 不抛异常：内部失败只记日志。
- 只补偿"写操作"，只读规则不需要补偿。

### 5.5 异常 vs RuleResult

| 场景                | 用哪个              |
| ------------------- | ------------------- |
| 条件不满足          | `RuleResult.Reject` |
| 无福利，跳过        | `RuleResult.Skip`   |
| 主动阻止并回滚      | `RuleResult.Abort`  |
| 系统崩溃、DB 连不上 | `throw`（意外）     |
| 逻辑 bug            | `throw`（意外）     |

**`RuleResult` 表达业务结论，`throw` 表达意外。**

---

## 6. KSP 生成设计

### 6.1 生成产物

| 文件                                     | 内容                                    |
| ---------------------------------------- | --------------------------------------- |
| `ActionRegistry_Generated.kt`            | `actionId → ActionMeta`                 |
| `BeforeRegistry_Generated.kt`            | `actionId → List<SuspendRule>`          |
| `AfterRegistry_Generated.kt`             | `actionId → List<SuspendRule>`          |
| `ActionContextRequirements_Generated.kt` | `actionId → List<KClass>`（从规则反推） |
| `ContextRegistry_Generated.kt`           | 从 `@Provides` 生成注册器               |
| `<业务类名>Actions.kt`                   | 每个 `@Action` 一个同名门面函数         |

### 6.2 需求列表反推（核心）

KSP 扫描所有规则方法的参数，汇总每个 action 需要的类型：

```kotlin
// 规则
@Before("daily.sign") suspend fun a(userInfo: UserInfoDto) { ... }
@Before("daily.sign") suspend fun b(userTitle: UserTitle) { ... }
@After("daily.sign")  suspend fun c(result: SignResult, userInfo: UserInfoDto) { ... }

// 生成
object ActionContextRequirements_Generated {
    val requirements: Map<String, List<KClass<*>>> = mapOf(
        "daily.sign" to listOf(
            UserInfoDto::class,
            UserTitle::class,
        ),
    )
}
```

**`SignResult` 不在列表里**——它是返回值，引擎自动 `setResult`，不走 `@Provides`。

**动作作者完全不知道有这一步。**

### 6.3 `XxxActions` 门面生成

```kotlin
// 业务
class SignUsecase {
    @Action("daily.sign")
    suspend fun sign(event: GroupMessageEvent): SignResult { ... }
}

// KSP 生成
object SignUsecaseActions {
    suspend fun sign(event: GroupMessageEvent): SignResult =
        EngineHolder.engine.invokeFromEntry("daily.sign", event) { ctx ->
            SignUsecase().sign(event)
        }
}
```

**生成规则**：
- 对象名 = `<业务类简单名> + Actions`。
- 函数名 = 原方法名。
- 签名 = 原方法签名。
- 函数体 = `invokeFromEntry(actionId, event) { 原方法调用 }`。
- 只对 `@Action` 方法生成，未打注解的方法**不生成**（调用编译不过）。

### 6.4 参数注入决策树

```
参数是 ActionContext？
  ├─ 是 → 传 ctx
  └─ 否 → 是 @Action 返回值类型？
           ├─ 是 且 @After → ctx.result(XxxClass)!!
           ├─ 是 且 @Before → ❌ 编译报错
           └─ 否 → 类型在 @Provides 注册？
                    ├─ 是 → ctx.get(XxxClass)!!
                    └─ 否 → ❌ 编译报错
```

### 6.5 编译期校验清单

| 校验项                                         | 时机   |
| ---------------------------------------------- | ------ |
| `@Before`/`@After` 的 `value` 有对应 `@Action` | 编译期 |
| `@Action.id` 唯一                              | 编译期 |
| `@Before` 不引用返回值类型                     | 编译期 |
| 规则参数类型已在 `@Provides` 注册              | 编译期 |
| `compensate` 类实现 `Compensation`             | 编译期 |
| 规则方法返回值是 `Unit` 或 `RuleResult`        | 编译期 |

### 6.6 KSP Processor 骨架

```kotlin
class ActionProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val actions = resolver.getSymbolsWithAnnotation(Action::class.qualifiedName!!)
            .filterIsInstance<KSFunctionDeclaration>()
        val befores = resolver.getSymbolsWithAnnotation(Before::class.qualifiedName!!)
            .filterIsInstance<KSFunctionDeclaration>()
        val afters = resolver.getSymbolsWithAnnotation(After::class.qualifiedName!!)
            .filterIsInstance<KSFunctionDeclaration>()
        val provides = resolver.getSymbolsWithAnnotation(Provides::class.qualifiedName!!)
            .filterIsInstance<KSFunctionDeclaration>()

        validate(actions, befores, afters, provides)

        generateActionRegistry(actions)
        generateBeforeRegistry(befores)
        generateAfterRegistry(afters)
        generateRequirements(befores, afters)
        generateContextRegistry(provides)
        generateActionsFacades(actions)

        return emptyList()
    }
}
```

---

## 7. 完整示例：签到

### 7.1 动作

```kotlin
class SignUsecase {
    @Action("daily.sign")
    suspend fun sign(event: GroupMessageEvent): SignResult {
        return doSign(event.sender.id)
    }
}
```

### 7.2 规则

```kotlin
object SignRules {

    @Before("daily.sign", order = 1)
    suspend fun checkAlreadySigned(userInfo: UserInfoDto): RuleResult =
        if (userInfo.signedToday) RuleResult.Reject("ALREADY_SIGNED", "今日已签到")
        else RuleResult.Continue

    @Before("daily.sign", order = 5)
    suspend fun applyTitleBonus(userTitle: UserTitle, ctx: ActionContext) {
        titleService.rewardModifier(userTitle)?.let { ctx.addModifier(it) }
    }

    @Before("daily.sign", order = 10)
    suspend fun applyWelfare(userInfo: UserInfoDto, ctx: ActionContext) {
        if (!welfareService.isActive(userInfo.userId)) return
        ctx.addModifier(welfareService.modifier(userInfo.userId))
    }

    @Before("daily.sign", order = 20, compensate = ConsumeCompensation::class)
    suspend fun consumeItem(userBackpack: UserBackpack, ctx: ActionContext) {
        val item = backpackService.signConsumeItem(userBackpack) ?: return
        userBackpack.consume(item, 1)
        ctx.consumed = item to 1
    }

    @Before("daily.sign", order = 30)
    suspend fun checkFinalCondition(userInfo: UserInfoDto): RuleResult =
        if (!userInfo.someCondition) RuleResult.Abort("条件不满足，本次签到取消")
        else RuleResult.Continue

    @After("daily.sign", order = 100)
    suspend fun settle(
        result: SignResult,
        userInfo: UserInfoDto,
        ctx: ActionContext,
    ) {
        val base = result.baseReward.toDouble()
        val final = ctx.modifiers().applyTo(base)
        rewardService.grant(userInfo.userId, final.toLong())
    }
}
```

### 7.3 补偿器

```kotlin
class ConsumeCompensation(
    private val backpackService: BackpackService,
) : Compensation {
    override suspend fun compensate(ctx: ActionContext, cause: Throwable) {
        val consumed = ctx.consumed ?: return
        backpackService.add(ctx.userId, consumed.first, consumed.second)
    }
}
```

### 7.4 类型注册

```kotlin
object SignProviders {
    @Provides
    suspend fun userInfo(userId: Long): UserInfoDto = userService.load(userId)

    @Provides
    suspend fun userTitle(userId: Long): UserTitle = titleService.load(userId)

    @Provides
    suspend fun userBackpack(userId: Long): UserBackpack = backpackService.load(userId)
}
```

### 7.5 入口

```kotlin
@MessageAuthorize(text = ["签到"], messageMatching = MessageMatchingEnum.EQUAL)
suspend fun sign(event: GroupMessageEvent) {
    SignUsecaseActions.sign(event)
}
```

### 7.6 执行流程

```
用户发送"签到"
    ↓
sign(event) → SignUsecaseActions.sign(event)
    ↓
引擎装配 ctx：UserInfoDto, UserTitle, UserBackpack
    ↓
before 1: 已签到？→ Reject → 回滚（无）→ 抛 ActionRejected
before 5: 称号加成 → ctx.addModifier
before 10: 福利加成 → ctx.addModifier
before 20: 消费道具 → ctx.consumed = ...
before 30: 最终条件 → Abort → 逆序补偿 → ConsumeCompensation 退回道具
    ↓（若全部通过）
动作本体：doSign → SignResult
    ↓
after 100: settle → 读 modifiers → 发奖
```

---

## 8. 新增系统流程（验证扩展性）

**需求**：新增"连续签到 7 天，收益 ×1.5"。

**第 1 步：注册类型**（装配维护者）

```kotlin
@Provides
suspend fun signStreak(userId: Long): SignStreak = signService.streak(userId)
```

**第 2 步：写规则**（规则作者）

```kotlin
@Before("daily.sign", order = 7)
suspend fun applyStreakBonus(streak: SignStreak, ctx: ActionContext) {
    if (streak.days >= 7) {
        ctx.addModifier(RewardModifier.Multiply("streak.7d", 1.5))
    }
}
```

**结束。**

**不需要改**：`SignUsecase`、`@Action`、引擎、装配器、入口、其他规则。

---

## 9. 工程约定

### 9.1 命名约定

| 项       | 约定                                        |
| -------- | ------------------------------------------- |
| actionId | `<域>.<动作>`，如 `daily.sign`、`game.fish` |
| 门面对象 | `<业务类名>Actions`                         |
| 规则类   | `<域>Rules`，如 `SignRules`                 |
| 提供者类 | `<域>Providers`，如 `SignProviders`         |
| 补偿器   | `<动作>Compensation`                        |

### 9.2 order 约定

见 3.4 节。

### 9.3 事务

若所有 service 共用同一数据库，**优先用数据库事务包裹 `invokeFromEntry`**，能省掉大部分手写补偿。只有跨系统（内存 + DB）才需要 `Compensation`。

### 9.4 业务类实例化

生成代码假设 `SignUsecase()` 无参构造。约定：

- 业务类无状态，依赖通过 `ServiceLocator` / `object` 单例获取。
- 若必须注入，改用 DI 容器并调整 KSP 生成模板。

---

## 10. 落地路线

| 阶段   | 内容                                                         | 产出       |
| ------ | ------------------------------------------------------------ | ---------- |
| **P0** | 手写 `ActionContext`/`ActionEngine`/`RuleResult`/三个 Registry（手动填），跑通签到 | 规则链能跑 |
| **P1** | KSP 生成三个 Registry + Requirements                         | 注册自动化 |
| **P2** | KSP 生成 `XxxActions`，入口换成一行                          | 入口零样板 |
| **P3** | 加 `RuleResult` + `Unit` 默认 + 补偿器                       | 安全可控   |
| **P4** | 加编译期校验（6 项）                                         | 编译期安全 |
| **P5** | 加 `@Provides` 生成 `ContextRegistry`                        | 装配自动化 |
| **P6** | 多模块支持（如有）                                           | 可拆分     |

**每阶段可独立验证，不建议一次写完。**

---

## 11. 已知限制与演进

### 11.1 限制

| 限制                 | 说明                     |
| -------------------- | ------------------------ |
| 不支持运行时动态注册 | KSP 编译期决定           |
| 不支持无感拦截       | 必须调用 `XxxActions`    |
| 不支持同类型多实例   | 第一版无 `@Named`        |
| 业务类需无参构造     | 或走 DI                  |
| 多模块需聚合         | 每个 module 生成自己的表 |

### 11.2 演进方向

- `@Named` 限定符：支持同类型多实例。
- `@Action` + `@MessageAuthorize` 联动：连入口都不用写。
- KSP 增量优化：每个 action 独立文件。
- 运行时动态兜底：字符串式 `MethodHandle` 入口。

---

## 12. 附录：术语表

| 术语         | 含义                               |
| ------------ | ---------------------------------- |
| Action       | 被规则链包裹的业务动作             |
| Rule         | 横切逻辑单元                       |
| Provider     | 类型加载声明                       |
| Context      | 执行上下文                         |
| RuleResult   | 规则结论                           |
| Compensation | 副作用回滚器                       |
| Facade       | KSP 生成的入口门面（`XxxActions`） |
| Registry     | KSP 生成的注册表                   |

