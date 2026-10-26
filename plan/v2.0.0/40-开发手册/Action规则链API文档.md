# Action 规则链 API 文档

> 状态：阶段五产物（2026-10-26）。本文与源码对齐，验证依据为
> `:economy-action:test`（10 项）、`:economy-action-ksp:test`（7 项）、
> `:economy-core:test` / `:economy-main:test`（含 `ActionChainIntegrationTest`）。
> 工具链：Kotlin `1.9.20`、KSP `1.9.20-1.0.14`、JVM target `11`。
>
> 配套文档：
> - 为什么这么设计：[KSP 驱动的 Action 规则链框架设计](../../../docs/KSP%20%E9%A9%B1%E5%8A%A8%E7%9A%84%20Action%20%E8%A7%84%E5%88%99%E9%93%BE%E6%A1%86%E6%9E%B6%20%C2%B7%20%E8%AE%BE%E8%AE%A1%E6%96%87%E6%A1%A3.md)
> - 五个阶段怎么落地：[Action 规则链系统开发手册](Action规则链系统开发手册.md)
> - 怎么迁一个功能（含 28 条易错点）：[签到功能规则链改造实录](签到功能规则链改造实录.md)
>
> 本文只回答一个问题：**写业务代码时，可以依赖哪些 API、它们的边界在哪里**。
> 设计草案、已废弃表述、未实现能力一律不进本文；差异清单见 §11。

---

## 0. 一页速查

| 你想做的 | 写法 | 落在哪 |
| --- | --- | --- |
| 声明一个可编排的动作 | `@Action("core.sign")` | `feature/<功能>/<Feature>Usecase.kt`（唯一位置） |
| 动作前校验/计算，可拦截 | `@Before(actionId = "core.sign", order = 10)` | `feature/<功能>/<Feature>Rules.kt` |
| 动作后结算/日志 | `@After(actionId = "core.sign", order = 10)` | 同上 |
| 给规则/动作提供一份复用数据 | `@Provider(qualifier = "signer", actionId = "core.sign")` | `feature/<功能>/<Feature>Provider.kt`（真用到才建） |
| 从规则里取数据 | 参数写成 `@FromProvider("signer") actor: SignActor` | 自动注入 |
| 拦截并回一句话 | `return ActionResult.EndWithMessage("你已经签到过了哦!")` | 规则返回值 |
| 拦截但不说话 | `return ActionResult.EndSilently` | 规则返回值 |
| 读动作结果 | `context.actionResult(SignOutcome::class) ?: return` | `@After` 里 |
| 传中间数据 | `context.put(SignReward::class, reward)` / `context.get(SignReward::class)` | before → action |
| 指令层调用 | `SignUsecaseActions.sign(event)` | `economy-main/action/` |

七个名词，一次说清：

| 名词 | 一句话 |
| --- | --- |
| Action | 一个可被规则链包住的动作，一个 `actionId` 对应一个 `suspend` 函数 |
| Rule | 挂在 actionId 上的横切步骤，分 `BEFORE` / `AFTER` 两段 |
| Provider | 按「类型 + 限定名」提供运行时对象的 `suspend` 函数，一次调用内缓存 |
| ActionContext | 单次调用的容器：事件、中间值、动作返回值、Provider 缓存 |
| ActionEngine | 不可变的调度表，负责顺序、短路、校验 |
| ActionRuntime | 进程级引擎持有者，生成的门面只认它 |
| ActionModule | 一个 Gradle 模块的寄存器，由 `economy-main` 显式聚合 |

---

## 1. 模块与接入

### 1.1 两个模块

| 模块 | 角色 | 是否进插件 jar |
| --- | --- | --- |
| `economy-action` | 注解 + 运行时（`ActionContext` / `ActionResult` / `ActionEngine` / `ActionRuntime`） | **是**，业务模块用 `api(...)` 依赖 |
| `economy-action-ksp` | KSP Processor（生成 facade 与模块描述） | **否**，只作编译期 `ksp(...)` |

依赖方向是单向的：`core / game → economy-action`；`economy-action` 不依赖 core / game / main / HuYanAuthorize / Mirai。
引擎把事件当作 `Any` 传递，回复出口由宿主注入，所以运行时模块可以在纯 JVM 测试里跑完整条链。

### 1.2 业务模块接入三件套

```kotlin
// economy-core/build.gradle.kts
plugins {
    id("com.google.devtools.ksp")
}

// 规则链代码生成：facade 生成在声明包内，模块描述生成在 actionchain.generated 包。
ksp {
    arg("actionchain.moduleId", "economy-core")
}

dependencies {
    api(project(":economy-action"))
    ksp(project(":economy-action-ksp"))
}
```

`economy-game` 同样配置，`moduleId = "economy-game"`。**`economy-main` 目前不应用该 Processor**，只聚合生成结果。

### 1.3 KSP 参数

| 参数 | 默认值 | 作用 | 写错的后果 |
| --- | --- | --- | --- |
| `actionchain.moduleId` | `unnamed-module` | 模块标识 + 生成对象名前缀 | 两个模块都用默认值 → 生成类全限定名相同，main 聚合时撞类；启动日志也认不出是哪个模块 |
| `actionchain.generatedPackage` | `cn.chahuyun.economy.actionchain.generated` | 模块描述所在包 | main 找不到类，编译失败 |
| `actionchain.allowExternalActions` | `false` | 允许规则/Provider 引用**本模块未声明**的 actionId | 开着只会降级为 warning，不会真的替你校验；跨模块引用仍未实测 |

> `allowExternalActions` 的定位是「临时放行」，不是「支持跨模块」。首版约定 **Provider 与它服务的 Action 同模块**。

### 1.4 聚合与启动顺序

`economy-main/.../plugin/ActionChainBootstrap.kt`：

```kotlin
private val modules: List<ActionModule> = listOf(
    EconomyCoreActionModule.module,
    EconomyGameActionModule.module,
)

val engine: ActionEngine = ActionEngine(modules, resultHandler)   // 构造期完成全部校验
fun install()  { ActionRuntime.install(engine) }                  // onEnable 调用
fun shutdown() { ActionRuntime.uninstall() }                      // onDisable 调用
```

`HuYanEconomy.onEnable` 中的顺序（不可调换）：

```text
HibernateUtil.init → 各 Manager/Service init → EconomyLedgerProvider.init
  → ActionChainBootstrap.install()              // 必须在事件注册之前
  → AuthorizeServer.registerEvents("cn.chahuyun.economy.action")
```

原因：指令一触发就会走到 facade → `ActionRuntime.invoke`；引擎没装则抛
`IllegalStateException: ActionRuntime is not installed...`。

`onDisable` 里 `ActionChainBootstrap.shutdown()`，避免热重载后旧引擎里的规则引用到已卸载的状态。

### 1.5 新增一个业务模块的清单

1. `build.gradle.kts`：`id("com.google.devtools.ksp")` + `ksp(project(":economy-action-ksp"))` + `ksp { arg("actionchain.moduleId", "...") }`
2. `settings.gradle.kts` 注册模块
3. 业务模块 `api(project(":economy-action"))`
4. `ActionChainBootstrap.modules` 登记生成的 `XxxActionModule.module`
5. 启动日志确认：`Action 规则链装配完成：N 个模块，M 个 action -> ...`

漏掉第 4 步的症状很有欺骗性：**编译通过、测试通过、指令就是不生效**，日志里也没有该 action。

---

## 2. 注解参考

全部注解都是 `AnnotationRetention.SOURCE`：编译后不进 class 文件，运行期无法反射读取。
**注解只被 KSP 消费**，所有接线都靠生成代码。

### 2.1 `@Action`

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Action(val actionId: String)
```

| 项 | 规则 |
| --- | --- |
| 可标注位置 | `object` 的 `suspend` 成员函数；不支持顶层函数、扩展函数、普通 class 成员。写在 `companion object` 里技术上能生成，但门面名会变成 `CompanionActions`，不要这么用 |
| 可见性 | 至少 `internal`（`private` / `protected` 报错，因为生成的 facade 要调用它） |
| 参数 | `(event)` 或 `(event, context: ActionContext)`；第一个参数必须是事件对象，不能是 `ActionContext`；不允许 0 个或 3 个以上参数 |
| 返回值 | 任意，含 `Unit` 与可空类型；非 Unit 返回值会进 `ActionExecution<T>.value` 并写入 context |
| actionId | 非空、全局唯一；模块内重复 KSP 报错，跨模块重复在引擎构造期抛异常 |

```kotlin
object SignUsecase {
    @Action("core.sign")
    suspend fun sign(event: GroupMessageEvent, context: ActionContext): SignOutcome? { ... }

    @Action("core.sign.close")
    suspend fun closeSign(event: GroupMessageEvent) { ... }
}
```

> 约定：`actionId` 用 `领域.动作` 形式，核心经济 `core.*`、玩法 `game.*`。
> **一个功能只留一个主 usecase，`@Action` 只标在它身上**（`SignUsecase` 里 4 个动作是"同一功能的多入口"，不是多个功能）。

### 2.2 `@Before` / `@After`

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Before(val actionId: String, val order: Int = 100)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class After(val actionId: String, val order: Int = 100)
```

| 项 | 规则 |
| --- | --- |
| 可标注位置 | `object` 的 `suspend` 成员函数，可见性 ≥ `internal` |
| 参数 | 只能是 `ActionContext`，或**不可空的类类型**（由 Provider 注入，可用 `@FromProvider` 指定限定名）；**不能直接写事件类型** |
| 返回值 | 只能 `Unit` 或 `ActionResult` 及其子类型 |
| actionId | 必须指向已注册的动作；本模块没有该 actionId 时报错（除非开 `allowExternalActions`，那时只 warning） |
| order | 越小越先执行，默认 100；同 order 按 `ruleId` 字典序（= `声明对象全限定名#函数名`） |

`@Before` 与 `@After` 的差别只有两点：

- **`@Before` 可以拦掉动作本体**；`@After` 只能拦掉后续 after 规则。
- **动作没执行就没有 `@After`**（被 before 拦截、或动作抛异常，after 都不跑）。

```kotlin
object SignRules {
    @Before(actionId = "core.sign", order = 10)
    suspend fun rejectIfNotSignable(@FromProvider("signer") actor: SignActor): ActionResult =
        if (SignService.canSign(actor.userInfo, EconomyRuntime.config.reSignTime)) ActionResult.Continue
        else ActionResult.EndWithMessage("你已经签到过了哦!")

    @After(actionId = "core.sign", order = 10)
    suspend fun logCommitted(context: ActionContext) { ... }
}
```

order 约定（与签到样板一致，见 §11.1 修正说明）：

| order | 用途 | 签到实例 |
| --- | --- | --- |
| 1–9 | 开关、冷却、频率限制 | — |
| 10 | 资格校验（纯判断，无副作用） | `rejectIfNotSignable` |
| 20 | 奖励/参数计算（可带副作用，但必须排在纯校验之后） | `calculateReward` |
| 30–49 | 追加加成（节日翻倍、公会加成） | 预留 |
| 50–99 | 资源扣除、状态准备 | — |
| 100+ | 兜底/统计 | — |

`@After` 同样从 10 开始，按「记录 → 通知 → 统计」递增。**order 只决定顺序，不带来任何事务或回滚语义**。

### 2.3 `@Provider`

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Provider(
    val qualifier: String,          // 注意：没有默认值，必须显式写
    val actionId: String = "",      // 空串 = 全局可见
)
```

| 项 | 规则 |
| --- | --- |
| 可标注位置 | `object` 的 `suspend` 成员函数，可见性 ≥ `internal` |
| 参数 | `()` 或 `(context: ActionContext)`；其他签名报错 |
| 返回值 | 非空、非 `Unit`、必须是类类型（`T::class` 要做键） |
| qualifier | **必填**，想表达"未限定"要显式写 `qualifier = ""` |
| actionId | 空串 = 全局 Provider；非空 = 只对该动作可见（且该动作必须在本模块存在，否则报错） |

```kotlin
object SignProvider {
    const val SIGNER: String = "signer"

    @Provider(qualifier = SIGNER, actionId = "core.sign")
    suspend fun signer(context: ActionContext): SignActor {
        val event = context.event as? GroupMessageEvent ?: error("core.sign 只能由群消息触发")
        return SignActor(
            userInfo = UserCoreManager.getUserInfo(event.sender),
            user = event.sender,
            subject = event.subject,
            message = event.message,
            groupId = event.group.id,
        )
    }
}
```

### 2.4 `@FromProvider`

```kotlin
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class FromProvider(val qualifier: String = "")
```

只用于**规则参数**。两种误用会编译报错：标注 `ActionContext` 参数（"@FromProvider 不能标注 ActionContext 参数"）、
标注 Provider 函数自身的参数（"@FromProvider 只能用于规则参数"）。标在 **Action 本体参数**上不报错，但会被**静默忽略**——
那里本来就不做 Provider 注入，别指望它生效。

| 写法 | 行为 |
| --- | --- |
| 不写 `@FromProvider` | 按类型在可见范围内找；恰好 1 个候选才通过，0 个报"找不到"，多个报"歧义" |
| `@FromProvider("signer")` | 按「类型 + `signer`」找；没有则运行期抛"缺少 Provider" |
| `@FromProvider("")` | **等同于不写**（空串视为"未指定"），不会去匹配 `qualifier = ""` 的 Provider |

### 2.5 注解参数必须是编译期常量

KSP 读注解参数的方式是 `arguments.firstOrNull { it.name == "..." }?.value as? String`。
只有**编译期常量**（字面量或 `const val`）会被解析成 `String`；普通 `val`、函数调用、字符串拼接、`when` 表达式都拿不到。

拿不到之后的后果**按参数不同而不同**，这一条文档以前写错过（见 §11.4）：

| 位置 | 拿不到时的行为 | 是否报错 |
| --- | --- | --- |
| `@Action(actionId = X)` | `null` → 空串 | ✅ 报错「@Action 的 actionId 不能为空」 |
| `@Before/@After(actionId = X)` | `null` → 空串 | ✅ 报错「@Before/@After 的 actionId 不能为空」 |
| `@Provider(actionId = X)` | `null` → `takeIf { isNotBlank() }` → `null` | ❌ **静默变成全局 Provider**（最危险） |
| `@Provider(qualifier = X)` | `null` → `orEmpty()` → `""` | ❌ 静默变成空限定名，`@FromProvider("signer")` 找不到它 |
| `@FromProvider(qualifier = X)` | `null` → `orEmpty()` → `""` | ❌ 静默退化成"未指定"，多候选时报歧义 |

改完这些参数**必须打开生成物核对**，编译通过不代表解析成功。

---

## 3. 运行时 API 参考

### 3.1 `ActionContext`

一次调用一个实例，构造函数是 `internal`，业务侧只读取、不创建。

```kotlin
class ActionContext internal constructor(
    val actionId: String,
    val event: Any,
    private val providerResolver: suspend (ActionContext, KClass<*>, String?) -> Any,
)
```

| 成员 | 签名 | 说明 |
| --- | --- | --- |
| `actionId` | `String` | 本次调用的动作 id |
| `event` | `Any` | 入口原样传入的事件；规则里请 `as? 具体事件 ?: return` |
| `attrs` | `MutableMap<String, Any?>` | 字符串键的自由袋，建议键名带模块前缀（`fishing.cooldown`） |
| `put` | `<T : Any> put(type: KClass<T>, value: T, qualifier: String = "")` | 同「类型 + 限定名」重复 put 会覆盖 |
| `get` | `<T : Any> get(type: KClass<T>, qualifier: String = ""): T?` | 未命中或类型不符返回 `null`，不抛异常 |
| `provide` | `suspend fun <T : Any> provide(type: KClass<T>, qualifier: String? = null): T` | 走 Provider 解析；缺/歧义抛 `ActionConfigurationException` |
| `actionResult` | `<T : Any> actionResult(type: KClass<T>): T?` | 读动作**返回值**；动作未执行或类型不符返回 `null` |

三条通道，别混用：

| 通道 | 谁写 | 谁读 | 典型场景 |
| --- | --- | --- | --- |
| `put` / `get` | before 规则、动作 | 动作、after 规则 | before 算好的奖励对象交给动作（`SignReward`） |
| `provide` | 不需要谁写，Provider 自己加载 | 规则参数、动作 | 跨阶段共享的用户快照（`SignActor`） |
| `actionResult` | 只有引擎写（动作返回值） | after 规则 | 判断这次到底成没成（`SignOutcome?`） |

> 判据口诀：**结果是返回值，中间数据是 put，可复用数据是 Provider**。
> 用 `put` 传"成功与否"会出现"put 了但入账失败"的假成功，这也是签到改造中踩过的坑。

### 3.2 `ActionResult`

```kotlin
sealed interface ActionResult {
    data object Continue : ActionResult
    data object EndSilently : ActionResult
    data class EndWithMessage(val message: String) : ActionResult
}
```

| 值 | before 阶段 | after 阶段 |
| --- | --- | --- |
| `Unit`（函数不返回值） | 等价 `Continue` | 等价 `Continue` |
| `Continue` | 下一条 before | 下一条 after |
| `EndSilently` | 停止：剩余 before、动作、after 全不执行，不发消息 | 停止剩余 after，不发消息 |
| `EndWithMessage(msg)` | 同上，并发一条文本 | 同上，并发一条文本 |

- 没有 `Rollback` / `Skip` / `Abort` 分支；框架**不提供自动回滚或补偿**。
- `EndSilently` / `EndWithMessage` 是正常业务结束，不是异常；真正的程序错误继续用异常表达。

### 3.3 `ActionExecution<T>`

```kotlin
data class ActionExecution<T>(
    val actionId: String,
    val result: ActionResult,
    val actionExecuted: Boolean,
    val value: T?,
)
```

| 字段 | 语义 |
| --- | --- |
| `result` | 最终控制流；动作正常跑完且 after 全放行时为 `Continue` |
| `actionExecuted` | 动作本体是否真的执行过；被 before 拦截或动作抛异常时为 `false`（抛异常时入口直接收到异常） |
| `value` | 动作返回值；未执行时为 `null`。动作返回 `Unit` 时这里是 `Unit` 实例，不是 `null` |

### 3.4 `ActionResultHandler`

```kotlin
fun interface ActionResultHandler {
    suspend fun handle(context: ActionContext, result: ActionResult.EndWithMessage)
}
```

- 引擎**每次调用最多调用一次**，且只在最终结果是 `EndWithMessage` 时调用，实现方不需要去重。
- 运行时模块不认识 Mirai，发送由宿主实现。当前实现：

```kotlin
ActionResultHandler { context, result ->
    val event = context.event
    if (event is MessageEvent) event.subject.sendMessage(result.message)
    else Log.warning("规则链: action '${context.actionId}' 返回 EndWithMessage，但事件 ... 不支持直接回复，消息已丢弃")
}
```

- 框架**不检查也不去重动作本体自己发的消息**：动作已经发过提示、before/after 又返回 `EndWithMessage`，玩家会收到两条。

### 3.5 `ActionEngine`

```kotlin
class ActionEngine(
    modules: Collection<ActionModule>,
    private val resultHandler: ActionResultHandler,
) {
    suspend fun <T> invokeFromEntry(
        actionId: String,
        event: Any,
        action: suspend (ActionContext) -> T,
    ): ActionExecution<T>
}
```

- 构造完成后**不可变**，可被多协程共享；模块传入顺序不影响结果。
- 构造期抛 `ActionConfigurationException` 的七类校验见 §4.4；调用期 actionId 未注册也抛它。
- 业务代码通常不直接调用：入口走 facade，测试/探针才直接 `invokeFromEntry`。

### 3.6 `ActionModule` / `ActionRule` / `ActionProvider`

```kotlin
data class ActionModule(
    val moduleId: String,
    val actionIds: Set<String>,
    val rules: List<ActionRule> = emptyList(),
    val providers: List<ActionProvider<out Any>> = emptyList(),
)

data class ActionRule(
    val actionId: String,
    val ruleId: String,
    val order: Int = 100,
    val stage: ActionRuleStage,
    val invoke: suspend (ActionContext) -> ActionResult,
) {
    companion object {
        fun before(actionId: String, ruleId: String, order: Int = 100, invoke: ...): ActionRule
        fun after(actionId: String, ruleId: String, order: Int = 100, invoke: ...): ActionRule
    }
}

class ActionProvider<T : Any>(
    val type: KClass<T>,
    val qualifier: String = "",
    val actionId: String? = null,   // null = 全局
    val load: suspend (ActionContext) -> T,
)

inline fun <reified T : Any> actionProvider(
    qualifier: String = "",
    actionId: String? = null,
    noinline load: suspend (ActionContext) -> T,
): ActionProvider<T>

enum class ActionRuleStage { BEFORE, AFTER }
class ActionConfigurationException(message: String) : IllegalArgumentException(message)
```

- `ruleId` 生成规则：`声明对象全限定名#函数名`，例如 `cn.chahuyun.economy.feature.sign.SignRules#calculateReward`。
  它有两个职责：日志排查、同 order 的次级排序键。
- 手写 `ActionModule` 只在测试或框架自身出现；业务模块一律用生成物。

### 3.7 `ActionRuntime`

```kotlin
object ActionRuntime {
    val isInstalled: Boolean
    fun install(engine: ActionEngine)
    fun uninstall()
    suspend fun <T> invoke(actionId: String, event: Any, action: suspend (ActionContext) -> T): ActionExecution<T>
}
```

- 生成的门面唯一依赖的入口，这样 facade 不必知道模块清单与构造参数（否则 facade 与 main 会互相依赖成环）。
- 引擎值 `@Volatile`；未安装时 `invoke` 抛 `IllegalStateException`（错误信息里直接写了修法）。
- 重复 `install` 会覆盖旧实例；`uninstall` 后任何 facade 调用都会抛异常，因此只能在确认不会再有事件进来时调用。

---

## 4. 执行语义

### 4.1 一次调用的七个步骤

```text
1. facade: ActionRuntime.invoke(actionId, event, block)
2. 引擎校验 actionId 已注册
3. 新建 ActionContext（本次独享 attrs / put 值 / Provider 缓存 / 动作返回值）
4. 按 (order, ruleId) 升序跑 before；遇到非 Continue 立刻停止
5. before 全放行 → 执行动作本体 → 返回值写入 context
6. 按 (order, ruleId) 升序跑 after；遇到非 Continue 立刻停止
7. 若最终 result 是 EndWithMessage → 调一次 handler；返回 ActionExecution
```

### 4.2 短路矩阵

| 阶段 | 结果 | 剩余 before | 动作 | after | 回复 |
| --- | --- | --- | --- | --- | --- |
| before | Continue | 继续 | 执行 | 执行 | 由后续决定 |
| before | EndSilently | 停止 | ❌ | ❌ | ❌ |
| before | EndWithMessage | 停止 | ❌ | ❌ | ✅ 一次 |
| 动作 | 抛异常 | — | ❌（抛出） | ❌ | ❌ |
| 动作 | 正常返回 | — | ✅ | 执行 | 由后续决定 |
| after | Continue | — | ✅ | 继续 | 由后续决定 |
| after | EndSilently | — | ✅ | 停止 | ❌ |
| after | EndWithMessage | — | ✅ | 停止 | ✅ 一次 |

### 4.3 异常与回复

- 动作抛出的异常**不被吞掉**，直接向上抛给 Mirai 事件处理路径；after 不执行。
- before/after 抛异常同样向上抛；引擎不做补偿。
- `EndWithMessage` 的发送发生在**全链结束之后**，不是在规则返回的那一刻。因此规则里的日志顺序与玩家看到消息的顺序可能不一致。

### 4.4 构造期校验（引擎）

| 类别 | 触发条件 | 异常信息（前缀） |
| --- | --- | --- |
| 动作重复 | 两个模块声明同一 actionId | `Duplicate actionId 'x' in modules a, b` |
| 规则悬空 | 规则引用的 actionId 没有任何模块声明 | `Rule 'r' references unregistered action 'a'` |
| 规则重复 | 同一 actionId 下同一 ruleId 出现两次 | `Duplicate ruleId 'r' for action 'a'` |
| Provider 悬空 | 局部 Provider 绑定了未声明的 actionId | `Provider T references unregistered action 'a'` |
| Provider 全局重复 | 两个全局 Provider 的「类型 + 限定名」相同 | `Duplicate global Provider key ...` |
| Provider 局部重复 | 同一动作内两个局部 Provider 同键 | `Duplicate Provider key ... for action 'a'` |
| 局部遮蔽全局 | 局部 Provider 与全局同键 | `Local Provider ... conflicts with a global Provider` |

**设计取向**：所有接线错误在 `ActionEngine(...)` 构造时爆，而不是等玩家触发指令。所以 `ActionChainBootstrap` 的 `engine` 是 `val`（启动即校验）。

### 4.5 Provider 缓存

- 缓存挂在 `ActionContext` 上，键为「类型 + Provider 自己声明的 qualifier」。
- 同一次调用内，无论多少条规则/动作写同一个 `@FromProvider("signer")`，Provider 函数只执行一次。
- **跨调用不共享**：下一次指令是新的 `ActionContext`，重新加载。所以 Provider 不能用来缓存跨请求状态。

---

## 5. Provider 解析规则

### 5.1 可见范围

```text
可见 Provider = 全局 Provider（actionId 为空）
             + 当前动作的局部 Provider（actionId == context.actionId）
```

其他动作的局部 Provider 完全不参与查找——这是"作用域"，不是"命名分组"。

### 5.2 匹配算法（等价伪代码）

```kotlin
val scoped = providers[context.actionId]     // 当前动作的局部
val global = providers[""]                   // 全局
val matching = (global + scoped).filter {
    it.type == 目标类型 && (qualifier == null || qualifier.isEmpty() || it.qualifier == qualifier)
}
when {
    matching.isEmpty() -> throw ActionConfigurationException("No Provider for T qualifier '' in action 'a'")
    matching.size > 1  -> throw ActionConfigurationException("Ambiguous Provider for T in action 'a'; specify a qualifier")
    else -> 命中缓存 or matching.single().load(context)
}
```

三条推论：

1. 同类型有多个来源时，**必须**用 `@FromProvider` 指定限定名，否则运行期报歧义。
2. 未指定限定名时会**忽略**限定名差异，所以"一个空 qualifier + 一个有 qualifier"的同类型 Provider 也算歧义。
3. 类型匹配用 `KClass`，**泛型实参不参与匹配**：`List<A>` 与 `List<B>` 都是 `List::class`，不能靠它们区分来源（见 §10）。

### 5.3 冲突处理（在构造期就爆）

| 情况 | 结果 |
| --- | --- |
| 全局内同「类型 + 限定名」 | 报错 |
| 同一动作内同「类型 + 限定名」 | 报错 |
| 不同动作的局部 Provider 同键 | 允许（作用域隔离） |
| 局部与全局同键 | 报错，**不允许局部遮蔽全局** |

### 5.4 跨模块

- KSP 只看本模块源码；规则引用了别的模块的 actionId 或 Provider，默认直接编译报错。
- 开 `actionchain.allowExternalActions = true` 后降级为 warning，**但引擎侧仍然会做聚合校验**（规则悬空、Provider 悬空都会在构造期抛）。
- 首版约定：**规则、Provider、Action 同模块**。跨模块只作为实验性放行，尚未真机验证。

---

## 6. 生成物参考

两类产物，位置固定，**一律不手改**。

### 6.1 facade：`<Owner>Actions`

- 位置：跟随**声明方包**（不受 `generatedPackage` 影响），与 `@Action` 所在 object 同包。
- 命名：object 简单名 + `Actions`。`SignUsecase` → `SignUsecaseActions`。
- 可见性：`public`；方法名与动作函数同名；**只暴露 event 参数**，context 由生成代码注入。

`SignUsecase.sign` 实际生成（节选自 `build/generated/ksp/main/kotlin/.../feature/sign/SignUsecaseActions.kt`，
为可读性把全限定类型名简写为短名）：

```kotlin
public object SignUsecaseActions {
    public suspend fun sign(event: GroupMessageEvent): ActionExecution<SignOutcome?> =
        ActionRuntime.invoke(
            actionId = "core.sign",
            event = event,
        ) { context ->
            cn.chahuyun.economy.feature.sign.SignUsecase.sign(event, context)
        }

    public suspend fun closeSign(event: GroupMessageEvent): ActionExecution<Unit> =
        ActionRuntime.invoke(
            actionId = "core.sign.close",
            event = event,
        ) { _ ->
            cn.chahuyun.economy.feature.sign.SignUsecase.closeSign(event)
        }
}
```

注意两点：动作声明 `(event)` 时 lambda 参数生成 `_`（避免 unused 警告）；返回类型保留可空性（`ActionExecution<SignOutcome?>`）。

### 6.2 模块描述：`<ModuleId>ActionModule`

- 位置：`actionchain.generated`（或 `generatedPackage` 指定值）。
- 命名：`moduleId.toPascalCase() + "ActionModule"`。`economy-core` → `EconomyCoreActionModule`。
- 结构：`object` + `val module: ActionModule`。下面是 `EconomyCoreActionModule` 的节选（类型名已简写）：

```kotlin
public object EconomyCoreActionModule {
    public val module: ActionModule = ActionModule(
        moduleId = "economy-core",
        actionIds = setOf("core.sign", "core.sign.close", "core.sign.open", "core.sign.refresh"),
        rules = listOf(
            ActionRule.before(
                actionId = "core.sign",
                ruleId = "cn.chahuyun.economy.feature.sign.SignRules#rejectIfNotSignable",
                order = 10,
                invoke = { context: ActionContext ->
                    cn.chahuyun.economy.feature.sign.SignRules.rejectIfNotSignable(
                        context.provide(SignActor::class, "signer"),
                    )
                },
            ),
            // ... 其余 6 条
        ),
        providers = listOf(
            actionProvider<SignActor>(
                qualifier = "signer",
                actionId = "core.sign",
                load = { context -> SignProvider.signer(context) },
            ),
        ),
    )
}
```

生成期已经按 `actionId, stage, order, ruleId` 排好序，并对重复 actionId / ruleId / Provider key 直接报错（**不会去重**）。
运行期排序仍以引擎为准（构造时重新按 `order, ruleId` 排），生成物里的顺序只是给人看的稳定输出。

### 6.3 命名规则汇总

| 生成物 | 规则 | 示例 |
| --- | --- | --- |
| facade 包 | 声明方包 | `cn.chahuyun.economy.feature.sign` |
| facade 名 | `<Object>Actions` | `SignUsecaseActions` |
| facade 方法 | 与 `@Action` 函数同名 | `sign(event)` |
| 模块描述包 | `actionchain.generated` | 同左 |
| 模块描述名 | `Pascal(moduleId) + ActionModule` | `EconomyCoreActionModule` |
| `ruleId` | `对象全限定名#函数名` | `cn.chahuyun.economy.feature.sign.SignRules#logCommitted` |
| 参数注入 | `context.provide(T::class[, "qualifier"])` | 见上 |

---

## 7. 案例：签到（`core.sign`）

选它的理由：4 个动作、7 条规则、1 个 Provider、1 次 before→action 数据传递、1 个 after 读返回值，**规则链的每一种能力都至少用到一次**，同时玩家可见行为与改造前逐字一致。

### 7.1 文件布局

```text
economy-core/src/main/kotlin/cn/chahuyun/economy/feature/sign/
├── SignUsecase.kt     # 4 个 @Action（动作本体）
├── SignRules.kt       # 2 个 @Before + 5 个 @After
├── SignProvider.kt    # 1 个 @Provider + SignActor 数据类
├── SignService.kt     # 对内领域逻辑：canSign / applySign / randomSignGold / signProp
└── SignReward.kt      # 功能私有模型：一次签到的奖励载体
```

`economy-main/action/SignAction.kt` 只剩「指令 → facade」映射 + 权限注解。

### 7.2 动作清单

| actionId | 触发指令 | 签名 | 是否有规则 |
| --- | --- | --- | --- |
| `core.sign` | 签到 / 打卡 / sign | `sign(event: GroupMessageEvent, context: ActionContext): SignOutcome?` | 2 before + 2 after |
| `core.sign.close` | 关闭 签到（管理员） | `closeSign(event: GroupMessageEvent)` | 1 after |
| `core.sign.open` | 开启 签到（管理员） | `openSign(event: GroupMessageEvent)` | 1 after |
| `core.sign.refresh` | 刷新签到（管理员） | `refreshSign(event: GroupMessageEvent)` | 1 after |

管理类动作没有可扩展的业务阶段，规则链上只有一条审计日志——**这本身就是「规则可以为空」的实证**：删掉那三条 `@After`，三个动作照样能跑。

### 7.3 规则矩阵

| 阶段 | order | 规则 | 做什么 | 结果 |
| --- | --- | --- | --- | --- |
| BEFORE | 10 | `rejectIfNotSignable` | `SignService.canSign` 纯判断（不推进状态） | Continue / `EndWithMessage("你已经签到过了哦!")` |
| BEFORE | 20 | `calculateReward` | `randomSignGold` + `signProp`，产出 `SignReward` | `context.put(SignReward::class, reward)` |
| — | — | `SignUsecase.sign` | 推进状态 → 入账 → 扣道具落库 → 拼文案 → 发图 | 返回 `SignOutcome?` |
| AFTER | 10 | `logCommitted` | 读 `SignOutcome` 记完成日志 | `actionResult(..) ?: return` |
| AFTER | 20 | `grantSignTitles` | 签到称号 + 首富检查 | `actionResult(..) ?: return` |
| AFTER | 10 | `logClosed` / `logOpened` / `logRefreshed` | 三条管理动作审计 | `event as? GroupMessageEvent ?: return` |

为什么 order 必须 10 < 20：`signProp` 里的补签卡会改 `userInfo.oldSignNumber`（已知副作用）。
若把它排在资格校验之前，重复签到虽然被拦，但用户连签数已经被改了一次。所以**纯校验在前、带副作用在后**，且间距留 10 方便插规则。

### 7.4 三个角色

**Provider：一份快照，三个阶段共享**

```kotlin
@Provider(qualifier = SignProvider.SIGNER, actionId = "core.sign")
suspend fun signer(context: ActionContext): SignActor {
    val event = context.event as? GroupMessageEvent
        ?: error("core.sign 只能由群消息触发，实际事件：${context.event::class.simpleName}")
    return SignActor(
        userInfo = UserCoreManager.getUserInfo(event.sender),   // 一次调用只查一次库
        user = event.sender,
        subject = event.subject,
        message = event.message,
        groupId = event.group.id,
    )
}
```

**Rules：资格判定 + 奖励计算 + 后置结算**

```kotlin
@Before(actionId = "core.sign", order = 10)
suspend fun rejectIfNotSignable(@FromProvider("signer") actor: SignActor): ActionResult {
    val signable = SignService.canSign(actor.userInfo, EconomyRuntime.config.reSignTime)
    return if (signable) ActionResult.Continue
    else ActionResult.EndWithMessage("你已经签到过了哦!")
}

@Before(actionId = "core.sign", order = 20)
suspend fun calculateReward(context: ActionContext, @FromProvider("signer") actor: SignActor) {
    val reward = SignReward(actor.userInfo)
    reward.param = RandomUtil.randomInt(0, 1001)
    reward.eventReplyAdd(MessageUtil.formatMessageChain(actor.message, "本次签到触发事件:"))
    SignService.randomSignGold(reward)
    SignService.signProp(reward)
    context.put(SignReward::class, reward)
}

@After(actionId = "core.sign", order = 10)
suspend fun logCommitted(context: ActionContext) {
    val outcome = context.actionResult(SignOutcome::class) ?: return
    Log.info("签到完成:群(${outcome.groupId}) 用户(${outcome.qq}) 连签 ${outcome.signNumber} 天,本次收益 ${outcome.gold.toPlainString()}")
}
```

**Usecase：只留不可拆的结算流水**

```kotlin
@Action("core.sign")
suspend fun sign(event: GroupMessageEvent, context: ActionContext): SignOutcome? {
    Log.info("签到指令:群(${event.group.id}) 用户(${event.sender.id})")   // 消费 event 参数

    val actor = context.provide(SignActor::class, SignProvider.SIGNER)    // 命中 @Before 阶段的缓存
    val userInfo = actor.userInfo

    if (!SignService.applySign(userInfo, EconomyRuntime.config.reSignTime)) return null   // 二次防御
    val reward = context.get(SignReward::class) ?: return null                            // 读 @Before(20) 产出
    val rewardAmount = UserWallet.money(reward.finalReward)                               // 两位 HALF_UP
    // 入账（UserWallet.credit）→ 扣道具 → 落库 → 拼文案 → 发图
    return SignOutcome(qq = userInfo.qq, gold = rewardAmount, signNumber = userInfo.signNumber, groupId = actor.groupId)
}
```

三个关键取舍：

1. **`context.provide` 在动作里也能用**，而且命中 before 阶段的缓存——所以「补签卡改过的连签数」能传到结算，这是不用 Provider 时做不到的。
2. **动作返回 `SignOutcome?` 而不是 `Boolean`**：after 规则只需一个判据，`null` 即"没签成"，不必再查库。
3. **两条提前返回路径（重复签到、入账失败）都返回 `null` 且自己发了提示**，after 规则看到 `null` 静默跳过——框架不替它去重。

### 7.5 一次成功签到的时序

```text
指令「签到」
  → SignAction → SignUsecaseActions.sign(event)
  → ActionRuntime.invoke("core.sign", event)
  → 引擎新建 context
  → @Before(10) rejectIfNotSignable
        provide(SignActor)  ── 首次加载：查库 1 次，缓存进 context
        return Continue
  → @Before(20) calculateReward
        provide(SignActor)  ── 命中缓存，不再查库
        put(SignReward)
  → SignUsecase.sign
        provide(SignActor)  ── 命中缓存（同一实例）
        get(SignReward)     ── 拿到 before 算好的奖励
        applySign → wallet.credit → 扣道具 → saveUserInfo → 发图
        return SignOutcome
  → context.setActionResult(SignOutcome)
  → @After(10) logCommitted        actionResult != null → 记日志
  → @After(20) grantSignTitles     actionResult != null → 检查称号
  → result == Continue，无 EndWithMessage，不调 handler
  → 返回 ActionExecution(actionExecuted = true, value = SignOutcome)
```

### 7.6 一次被拦截的调用

```text
指令「签到」（今天已签）
  → @Before(10) canSign == false
        return EndWithMessage("你已经签到过了哦!")
  → 引擎短路：不跑 @Before(20)、不执行 sign、不跑任何 @After
  → handler 发一条「你已经签到过了哦!」
  → 返回 ActionExecution(actionExecuted = false, value = null)
```

用户状态零变化：`canSign` 是纯判断，`applySign` 没执行，`signProp` 也没执行（所以补签卡不会被误消耗）。

### 7.7 生成物与测试断言（照抄时的核对锚点）

| 核对项 | 期望值 |
| --- | --- |
| `EconomyCoreActionModule.module.actionIds` | `{core.sign, core.sign.close, core.sign.open, core.sign.refresh}` |
| `core.sign` 规则条数 | 4（2 before + 2 after） |
| 全模块规则总数 | 7 |
| Provider 数量 / qualifier | 1 / `"signer"` |
| 规则 ruleId 前缀 | `cn.chahuyun.economy.feature.sign.SignRules#` |
| facade | `cn.chahuyun.economy.feature.sign.SignUsecaseActions`，4 个方法 |

`ActionChainIntegrationTest` 锁的就是这几条；新增/删除动作后必须同步，否则测试会红。

### 7.8 从一个新功能照抄的步骤

1. 建目录 `feature/<功能>/`，包名 `cn.chahuyun.economy.feature.<功能>`
2. `Usecase` 里按入口拆 `@Action`，同名指令合并成一个动作
3. `Rules.kt` 先建空壳（宣告扩展点），再往里加 `@Before(10)` 纯校验
4. 需要跨阶段共享数据 → 建 `Provider.kt`，定义一个数据类承载
5. 有副作用的前置逻辑放 order 20 之后；奖励/参数计算放这里
6. 动作用**返回值**交结果；`@After` 用 `actionResult(...) ?: return`
7. 指令层改调 facade（`XxxUsecaseActions.xxx(event)`），权限注解留在 action
8. 跑模块测试 + `buildPlugin`，打开生成物核对 actionIds/规则数/Provider

> 完整 7 步过程、7 个坑、28 条易错点见 [签到功能规则链改造实录](签到功能规则链改造实录.md)。

---

## 8. 编译期错误对照表

KSP 报错都带源码位置。按文案定位：

| 报错文案 | 原因 | 修法 |
| --- | --- | --- |
| `@Action 的 actionId 不能为空` | 参数不是编译期常量，或真写了空串 | 改成字面量 / `const val` |
| `@Action 方法必须是 suspend 函数` | 漏 `suspend` | 加 `suspend` |
| `@Action 只支持 (event) 或 (event, ActionContext) 两种签名` | 0 个或 3+ 个参数 | 多余输入并进 event 或 context |
| `@Action 的第一个参数必须是事件，不能是 ActionContext` | 参数顺序写反 | 事件在前 |
| `@Action 的第二个参数只能是 ActionContext` | 第二个参数写了别的类型 | 只允许 `ActionContext` |
| `... 只能标注在 object 的成员函数上` | 写在 class / 顶层 | 挪进 `object` |
| `... 只支持 object 成员函数（不支持顶层/扩展函数）` | 扩展函数 / 顶层 | 改成成员函数 |
| `... 方法必须至少是 internal 可见性` | `private` / `protected` | 提到 `internal` 或 `public` |
| `@Before/@After 的 actionId 不能为空` | 非常量参数 | 改成 `const val` / 字面量 |
| `规则函数必须是 suspend 函数` | 漏 `suspend` | 加 `suspend` |
| `规则函数只能返回 Unit 或 ActionResult` | 返回了 `Boolean` / `String` 等 | 改成 `Unit`，或返回 `ActionResult` |
| `规则参数必须是不可空的类类型，或 ActionContext` | 规则参数写了 `String?` / `Int` / 事件类型 | 用 `ActionContext` 取，或由 Provider 提供非空类 |
| `@FromProvider 不能标注 ActionContext 参数` | 给 context 参数加了注解 | 去掉注解 |
| `@FromProvider 只能用于规则参数` | 标到了 Provider 函数的参数上 | 去掉（标在 Action 参数上不会报错，但也不生效） |
| `Provider 函数必须是 suspend 函数` | 漏 `suspend` | 加 `suspend` |
| `Provider 只支持 () 或 (ActionContext) 两种签名` | 2+ 个参数 | 其他输入从 context 取 |
| `Provider 的唯一参数只能是 ActionContext` | 写了别的类型 | 改成 `ActionContext` |
| `Provider 必须返回不可空的非 Unit 类类型` | 返回 `Unit` / 可空 / 类型参数 | 返回具体非空类 |
| `actionId 'x' 在本模块重复声明` | 两个 `@Action` 同 id | 改名，或合并成一个动作 |
| `规则 ... 引用了不存在的 actionId 'x'` | 拼错，或规则在别的模块 | 改对；跨模块才考虑 `allowExternalActions` |
| `action 'x' 的 ruleId '...' 重复` | 同一函数同时标了 `@Before` 和 `@After` 指向同一 action | 拆成两个函数 |
| `Provider ... 绑定了本模块未声明的 actionId` | `actionId` 拼错或非常量 | 用 `const val`；确认动作在本模块 |
| `Provider 重复声明` | 同「类型 + 限定名 + actionId」声明两次 | 合并或改 qualifier |
| `Provider ... 与全局 Provider 冲突` | 局部与全局同键 | 改 qualifier，或去掉一方的 actionId |
| `规则 ... 的参数类型 T 存在多个 Provider` | 同类型多来源但没写限定名 | 加 `@FromProvider("...")` |
| `规则 ... 找不到类型 T 的 Provider` | Provider 没写、类型不匹配、限定名不符 | 核对类型与 qualifier 字符串 |

---

## 9. 运行期异常对照表

| 异常 | 触发时机 | 典型根因 |
| --- | --- | --- |
| `IllegalStateException: ActionRuntime is not installed...` | 任何 facade 调用 | `ActionChainBootstrap.install()` 没跑 / 跑在事件注册之后 / 已被 shutdown |
| `ActionConfigurationException: Unregistered action 'x'` | `invokeFromEntry` | facade 与模块描述不同步（增量编译残留），或手写了不存在的 id |
| `ActionConfigurationException: No Provider for T qualifier 'q' in action 'a'` | 规则参数解析 | 限定名拼错、Provider 的 `actionId` 不等于当前动作、非常量导致 qualifier 变空串 |
| `ActionConfigurationException: Ambiguous Provider for T in action 'a'` | 规则参数解析 | 同类型多候选且没写限定名 |
| `ActionConfigurationException: Duplicate actionId 'x' in modules ...` | 引擎构造 | 两个模块用了同一 actionId（或 `moduleId` 生成对象重名） |
| `ActionConfigurationException: Rule 'r' references unregistered action 'a'` | 引擎构造 | 规则所在模块登记了，但动作所在模块没登记进 `ActionChainBootstrap.modules` |
| `ActionConfigurationException: Local Provider ... conflicts with a global Provider` | 引擎构造 | 同一份数据既声明了全局 Provider，又声明了局部 Provider |

排查顺序建议：**先看启动日志**（`Action 规则链装配完成：N 个模块，M 个 action -> ...`，模块与动作清单都打出来了）→ 再看生成物 → 最后才怀疑业务代码。

---

## 10. 当前限制（明确不承诺）

| 限制 | 说明 | 影响 |
| --- | --- | --- |
| 无自动回滚 / 补偿 | 没有 `Rollback` 结果、没有事务包装 | 资源扣减类规则必须自己设计失败处理 |
| 无运行时动态注册 | 模块清单是编译期生成 + 启动期显式聚合 | 不能通过配置开关增删规则 |
| `EndWithMessage` 仅文本 | 只承载 `String` | 要发图片/消息链得在 `ActionResultHandler` 或动作里自己做 |
| 框架不去重消息 | 动作自己发的消息与规则回复互不知情 | 同一条逻辑消息别发两次 |
| Provider 不区分泛型实参 | 按 `KClass` 匹配 | `List<A>` / `List<B>` 无法作为两个来源 |
| 规则参数不支持可空 | 可空类型 KSP 直接拒绝；参数默认值写了也不会生效（生成代码总是传 Provider 值） | "可能没有"用 `context.get` 或定义结果类型 |
| 规则不能直接接收事件 | 事件类型参数会被当成 Provider 类型 | 一律 `context.event as? X ?: return` |
| `@Provider.qualifier` 必填 | 没有默认值 | "未限定"要显式 `qualifier = ""` |
| 跨模块引用未验证 | `allowExternalActions` 只降级告警 | 首版约定三件套同模块 |
| order 无事务语义 | 只是排序键 | 不要指望"order 靠前就能被恢复" |
| facade 只收 event | 不暴露 context | 入口无法预置上下文，需要预置就走 Provider 或共享服务 |
| 简单动作不要求入链 | 这是设计取向，不是遗漏 | 查询/帮助类指令继续直接调用 |

---

## 11. 本次核对与修正记录（2026-10-26）

写这份文档时逐条比对了源码，发现 6 处文档表述与实现不符，已就地修正。

### 11.1 设计文档的 order 区间与签到样板冲突

- 原文（设计文档 §7）：资格检查建议 `5–9`。
- 实际：签到资格校验是 `@Before(10)`，奖励计算 `@Before(20)`；手册 §9 与实录也按 10/20 记录。
- 处理：以**可运行的样板**为准，把区间表改为 `10 资格 / 20 计算 / 30-49 追加加成 / 50-99 资源准备 / 100+ 兜底`，并注明 order 只决定顺序。

### 11.2 设计文档称"相同 order 建议按源文件路径和声明位置排序"

- 原文（§4.2）：建议按源文件路径与声明位置稳定排序。
- 实际：阶段一契约第 6 节已决议为 **`ruleId`（对象全限定名#函数名）字典序**，引擎与 KSP 都按此实现。
- 处理：设计文档改为实际规则，并删掉"建议"措辞。

### 11.3 设计文档把 `ActionContext` 说成承载"Action 的显式参数"

- 原文（§5.1）：context 承载"Action 的显式参数"。
- 实际：`ActionContext` 只有 `actionId` / `event` / `attrs` / `put-get` 值 / Provider 缓存 / 动作返回值，**没有显式参数通道**；facade 也只传 event。
- 处理：改为"事件 + 中间值 + 返回值 + Provider 缓存"，并注明额外输入走 Provider 或 `put`。

### 11.4 「非 const 的注解参数静默降级成空串且不报错」过泛

- 原文（手册 §9 修正小节、实录 §7.C1）：非 `const` 会静默降级成空串且不报错。
- 实际：`@Action` 与 `@Before/@After` 的 `actionId` 变空串会**直接编译报错**；真正静默的是
  `@Provider(actionId = 非常量)` → **变成全局 Provider**、`@Provider/@FromProvider(qualifier = 非常量)` → 变空串。
- 处理：手册与实录改为按参数分别说明；本文 §2.5 给出完整对照表。

### 11.5 "`@Provider` 的 qualifier 必须写字面量"与后续修正自相矛盾

- 原文（手册 §9 阶段四陷阱）：qualifier 必须写字面量，`const val` 只用于代码侧。
- 实际：`const val SIGNER = "signer"` 已被正确解析（生成物即 `qualifier = "signer"`）。
- 处理：改为"必须是编译期常量（字面量或 `const val`）"，两处表述统一。

### 11.6 临时整理路线里的签到记录停留在中间态

- 原文（`临时整理路线.md` §8 修订小节）：`logCommitted` 为 `@After(order = 100)`，结果由 `context.put(SignOutcome)` 写入，奖励计算在 `SignManager`。
- 实际：`@After(order = 10)`；结果走**动作返回值** + `actionResult`；`SignManager` 已删除，奖励计算在 `SignService`。
- 处理：按最终形态改写该行。

此外同步修正：

- `docs/项目结构说明.md` 目录树中签到一行仍写着 `Manager`（已删除），改为 `Provider+Service`。
- `ActionExecution.value` 的 KDoc 原称"动作返回 Unit 时仍为 null"，实际是 `Unit` 实例，已改。
- `@FromProvider` 的 KDoc 原称"留空表示空限定名"，实际等价于"未指定"，已改。
