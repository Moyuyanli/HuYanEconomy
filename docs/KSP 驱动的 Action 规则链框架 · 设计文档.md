# KSP 驱动的 Action 规则链框架设计

> 状态：阶段一（骨架）、阶段二（运行时引擎）、阶段三（KSP 生成）、阶段四（core/game 接入与 main 聚合）已完成；阶段五 API 文档尚未完成，进度以开发手册为准
> 适用项目：HuYanEconomy 2.x
> 目标：为包含多步骤业务流程、需要前置/后置规则编排的动作提供编译期生成的规则链；简单动作继续直接调用。

本文以当前项目的模块结构和 Kotlin/KSP 版本为依据。规则链负责业务流程编排，不替代 Mirai 事件、HuYanAuthorize 权限系统、数据层事务或现有领域事件。

阶段一骨架决议见[Action 骨架契约](../plan/v2.0.0/40-%E5%BC%80%E5%8F%91%E6%89%8B%E5%86%8C/Action%E9%AA%A8%E6%9E%B6%E5%A5%91%E7%BA%A6.md)；本文描述总体设计，契约文档记录进入实现前已确定与仍待决议的 API 边界。

## 1. 目标与边界

### 1.1 目标

- 复杂业务动作可用 `@Action` 声明，并由 KSP 生成统一代理入口。
- `@Before` / `@After` 规则按 actionId 关联、排序和执行。
- Provider 可按类型与限定名装配参数；同类型的不同业务对象可以明确选择不同数据来源。
- Action、规则、Provider 和生成调用保持 `suspend`，不使用运行时反射。
- 编译期尽可能检查 actionId、参数类型、Provider 唯一性和规则签名。
- 规则链可以分阶段接入，直接动作不需要为了统一形式而添加 `@Action`。

### 1.2 非目标

- 不替代 `@MessageAuthorize`、命令匹配、权限校验或 Mirai 事件订阅。
- 不自动回滚任意数据库、内存、事件广播、消息发送或协程副作用。
- 第一版不支持运行时动态注册规则。
- 不要求所有指令都迁移到规则链。

### 1.3 规则链适用判断

适合标记 `@Action` 的场景：动作包含多个前置条件、多个可插拔加成/资源操作、需要统一后置结算，或流程拆分能显著改善可读性。

不需要标记的场景：简单查询、帮助、管理开关、直接委托单个 service/usecase 的命令。它们继续由 Action 入口直接调用现有业务代码。

## 2. 项目目标结构

```text
economy-main/action
  Mirai 命令与权限入口（HuYanAuthorize）
        |
        +-- 复杂动作 -> 生成的 XxxActions facade
        |                  |
        |                  v
        |          economy-core / economy-game
        |          object usecase + rules + providers
        |
        +-- 简单动作 ---------------------> 业务调用
                                           |
                                           v
                               economy-data / economy-base
```

模块依赖建议：

```text
economy-core  ──depends on──> economy-action
economy-game  ──depends on──> economy-action

economy-action-ksp ──KSP processor applied to──> economy-core / economy-game
                                                   (and economy-main if needed)

economy-main ──aggregates generated ActionModule descriptors──> economy-core / economy-game
```

`economy-action-ksp` 是编译期插件，由 `economy-core`、`economy-game`（以及确有 Action 时的 `economy-main`）应用；它不是业务模块的运行时依赖。

其中 `economy-action` 是运行时基础库；`economy-action-ksp` 是编译期处理器，不应进入 Mirai 插件运行时。KSP 插件版本与 Kotlin 版本保持匹配。当前项目 Kotlin 为 `1.9.20`、KSP 为 `1.9.20-1.0.14`、JVM target 为 11；首版沿用这些版本，不因引入规则链而升级。

目标业务结构是 `action -> usecase/rules -> data`。`economy-core` 和 `economy-game` 中的 usecase/rules 依领域放置；新框架不要求保留原有每个 service/manager/usecase 层级。是否合并现有类由迁移任务逐个判断，不在引擎中强制规定。

## 3. 概念与执行流程

### 3.1 概念

| 概念 | 职责 |
| --- | --- |
| Action | 可选的复杂业务流程入口，由 `@Action(id)` 标记 |
| Rule | 横切流程步骤，由 `@Before` 或 `@After` 绑定 actionId |
| Provider | 为规则参数提供数据的 suspend 函数 |
| ActionContext | 一次执行的上下文，包含 actionId、事件、参数和中间属性 |
| ActionEngine | 执行规则、动作本体及后置规则 |
| Generated facade | KSP 为带注解的 object 方法生成的调用入口 |
| ActionModule | 一个 Gradle 业务模块的 KSP 生成描述，由 main 聚合 |

### 3.2 流程

```text
HuYanAuthorize Action
  -> XxxActions.method(event)
  -> 创建 ActionContext
  -> 按 order 执行 Before
       -> Continue: 执行下一规则
       -> EndSilently / EndWithMessage: 结束本次 Action
  -> 调用 object 中的 Action 方法
  -> 按 order 执行 After
  -> 将执行结果交还入口适配层
```

只有复杂流程通过生成 facade；没有 `@Action` 的方法不生成代理，保持原有调用方式。

## 4. Action、规则和动作结果

### 4.1 Action 的类和签名

业务类统一使用 Kotlin `object`。KSP 生成代码直接调用 object，不构造业务类、不假设无参构造，也不引入 DI 容器。

首版签名约定：

- 常见形式为单个 Mirai event 参数。
- 确实需要上下文扩展能力时，业务 Action 可声明 `event, ctx`；`ctx` 由引擎创建并由生成代码注入，不由 Mirai 入口传入。
- 不为少量多参数历史签名扩大处理器复杂度；迁移时优先把额外输入归并到 event 或 `ActionContext`。
- 生成 facade 的公开签名只暴露入口参数（通常是 event），不会暴露引擎内部的 `ActionContext`。

示意：

```kotlin
object TransferUsecase {
    @Action("bank.transfer")
    suspend fun transfer(event: MessageEvent, ctx: ActionContext) {
        // 业务动作本体
    }
}
```

### 4.2 注解草案

以下为方案形态；阶段一已按此落地，实际属性名以 `economy-action/.../annotation/ActionAnnotations.kt` 为准：

```kotlin
@Target(AnnotationTarget.FUNCTION)
annotation class Action(val actionId: String)

@Target(AnnotationTarget.FUNCTION)
annotation class Before(val actionId: String, val order: Int = 100)

@Target(AnnotationTarget.FUNCTION)
annotation class After(val actionId: String, val order: Int = 100)
```

`actionId` 必须唯一。规则引用的 actionId 必须存在；同一 action 的规则按 `order` 升序执行。相同 order 的稳定顺序应由生成器定义，建议按源文件路径和声明位置稳定排序，避免依赖 KSP 返回顺序。

### 4.3 动作结果语义

业务结果先收敛为以下候选：

| 结果 | 语义 |
| --- | --- |
| `Continue` | 当前规则完成，继续执行后续规则或动作本体 |
| `EndSilently` | 结束本次 Action，不发送回复 |
| `EndWithMessage(message)` | 结束本次 Action，由结果处理端口交给上层发送回复 |
| 回滚 | 不属于首版结果 API；待范围和时机设计完成后再加入 |

`EndSilently` 和 `EndWithMessage` 是业务流程的正常结束，不是异常。意外的数据库错误或程序错误仍通过异常表达。

`EndWithMessage` 首版先限定为普通文本，由运行时 `ActionResultHandler` 端口交给上层 Mirai 适配发送。规则引擎本身不直接依赖 Mirai Contact/MessageChain；复杂消息内容或图片回复后续按实际需要扩展。

一次 invocation 最多调用一次 `ActionResultHandler`。框架不检查或去重 Action 本体直接发送的消息；使用规则结果回复的 Action 应避免本体再发送同一逻辑消息。

规则返回结束结果后的行为：

- `Before` 返回结束结果：不执行剩余 before、动作本体或 after。
- 动作本体成功后执行 after；after 返回结束结果时停止剩余 after，并把该结果返回入口。
- 动作本体抛异常时不执行 after，异常交由现有异常处理路径。
- 回滚结果仅列为未来扩展，不在上述流程中产生实际副作用。

### 4.4 RuleResult 草案

```kotlin
sealed interface ActionResult {
    data object Continue : ActionResult
    data object EndSilently : ActionResult
    data class EndWithMessage(val message: String) : ActionResult
}
```

规则返回 `Unit` 时可由生成代码映射为 `Continue`；需要结束流程时显式返回结果。规则控制结果类型命名为 `ActionResult`。首版不保留 `Skip`、`Abort`、`Reject` 或可执行的 Rollback 分支。

Action 本体允许返回任意业务值，运行时以 `ActionExecution<T>` 表示规则结果、是否执行了 Action 本体及其返回值。Action 返回值保存于 context，供 After 规则读取；before 提前结束时 `actionExecuted` 为 false，不产生业务返回值。

## 5. ActionContext 与 Provider

### 5.1 ActionContext

`ActionContext` 表示单次 Action 执行，可承载：

- `actionId` 和原始 Mirai event；
- Action 的显式参数；
- 规则间共享的类型化中间数据或 attrs；
- 动作本体的返回值（若需要供 after 使用）。

Context 生命周期仅限一次调用。不得把用户数据缓存到跨调用共享的可变对象中。事件类型和 Action 参数由生成 facade 放入 context；领域数据通过 Provider 获取。

### 5.2 Provider 的作用与作用域

Provider 是自行注册的数据装配函数，不局限于 `userId`。可根据 `ActionContext` 中的 event、用户、群、命令参数或已有属性查询任意需要的类型。

Provider 有两种可见范围：

1. **全局 Provider**：未绑定 actionId，可供所有 Action 使用。
2. **Action 局部 Provider**：声明绑定某个 actionId，只能供该 Action 的规则参数使用。

局部 Provider 的作用是限制可见性，而不只是命名分组。执行某个 Action 时，有效 Provider 集合为全局集合与该 Action 的局部集合；其他 Action 的局部 Provider 不参与查找。

### 5.3 类型与限定名

仅按返回类型无法区分同类型的不同来源。例如转账中，付款方账户和收款方账户都是 `EconomyAccount`，但加载方式和对象不同。因此 Provider 的解析键为：

```text
(actionId, valueType, qualifier)
```

概念性声明：

```kotlin
@Provider(qualifier = "fromUser", actionId = "bank.transfer")
suspend fun fromAccount(ctx: ActionContext): EconomyAccount = ...

@Provider(qualifier = "toUser", actionId = "bank.transfer")
suspend fun toAccount(ctx: ActionContext): EconomyAccount = ...
```

规则参数显式选择 Provider：

```kotlin
@Before("bank.transfer")
suspend fun validateAccounts(
    @FromProvider("fromUser") from: EconomyAccount,
    @FromProvider("toUser") to: EconomyAccount,
) {
    // from 与 to 类型相同，但由不同 Provider 装配
}
```

`@Provider` 和 `@FromProvider` 是便于说明的注解草案，名称可以调整。关键约定是 Provider 声明有返回类型、限定名和可选 actionId；规则参数在有歧义时显式声明限定名。

### 5.4 Provider 校验与冲突处理

- 同一有效作用域内，`(valueType, qualifier)` 必须唯一。
- 同一 Action 中全局 Provider 与局部 Provider 若产生相同解析键，视为冲突并报错；局部 Provider 不静默覆盖全局 Provider。
- 带限定名的参数必须匹配相同类型和限定名的 Provider。
- 无限定名参数仅在有效 Provider 集合中恰好有一个匹配类型时允许；零个时报缺失，多个时报歧义。类型存在多个来源时必须显式指定 qualifier。
- Provider 函数和规则函数均为 suspend；生成代码直接调用，不使用反射。
- Provider 只接收 `ActionContext`；同一 invocation 中首次解析后缓存，后续相同 `(类型, qualifier)` 读取复用该值。

示例转账参数的唯一绑定是：

```text
(bank.transfer, EconomyAccount, fromUser) -> fromAccount
(bank.transfer, EconomyAccount, toUser)   -> toAccount
```

### 5.5 属性与中间数据

只在规则间传递的一次性数据放入 `ActionContext`，不应为了临时值增加 Provider。优先使用有类型的 context 扩展或独立数据类；若使用字符串 attrs，应集中定义 key，避免拼写错误。

## 6. KSP 生成与多模块

### 6.1 模块职责

建议新增：

- `economy-action`：注解、ActionContext、动作结果、运行时引擎及模块描述接口。
- `economy-action-ksp`：KSP SymbolProcessor 和代码生成逻辑，只作为编译期依赖。

`economy-action` 不依赖 `economy-core`、`economy-game` 或 HuYanAuthorize。Mirai 事件由业务模块以类型参数传入；若引擎需要发送消息，由 `economy-main` 适配。

KSP Processor 通常不能在一个模块中扫描其他模块的源码，因此采用模块级生成：

```text
economy-core KSP -> CoreActionModule_Generated
economy-game KSP -> GameActionModule_Generated
economy-main KSP -> MainActionModule_Generated（若 main 自身定义 Action）
                           |
                           v
economy-main 初始化时显式聚合模块描述并创建 ActionEngine
```

每个业务模块只扫描并生成本模块的 Action、规则、Provider 和 facade。main 负责组合模块描述；不能假设某个 KSP Processor 一次扫描整个 Gradle 工程。

### 6.2 Provider 跨模块可见性

同模块内，KSP 可直接校验规则参数与 Provider 声明。跨模块引用不能假设 Processor 能读取依赖模块的 Kotlin 源码。

生成的 `ActionModule` 描述应包含 Provider 的元数据（Action 范围、返回类型、限定名）和实际加载函数。main 聚合时检查：

- Action ID 是否重复；
- 同一有效作用域内 Provider key 是否重复；
- 规则要求的 Provider key 是否存在。

实现阶段需用当前 KSP 版本验证生成类的元数据是否足以完成消费模块的编译期校验。若依赖模块生成信息不能被 KSP 稳定读取，首版采用显式模块聚合校验或模块本地规则/Provider 配对，不以反射兜底。

### 6.3 生成产物

每个业务模块至少生成一个模块描述，包含：

- 该模块的 actionId 和 facade 调用；
- actionId 对应的 before/after 规则及稳定排序；
- Provider 元数据和类型限定加载函数；
- 规则参数到 Provider 的确定绑定。

不要求必须拆成多个全局 Map 源文件。优先生成结构清楚、可由 main 聚合的模块级对象，避免源文件名和注册表数量成为公共契约。

每个带 `@Action` 的 object 方法生成同签名 facade，例如：

```kotlin
object TransferUsecase {
    @Action("bank.transfer")
    suspend fun transfer(event: MessageEvent, ctx: ActionContext) { ... }
}

// 生成代码示意
object TransferUsecaseActions {
    suspend fun transfer(event: MessageEvent) =
        ActionRuntime.invoke("bank.transfer", event) { ctx ->
            TransferUsecase.transfer(event, ctx)
        }
}
```

调用未生成的 facade 应编译失败。简单动作不标注 `@Action`，也不生成 facade。

### 6.4 编译期校验

首版目标校验：

- `@Action.id` 非空且在聚合结果中唯一；
- `@Before` / `@After` 引用的 actionId 存在；
- Action、规则和 Provider 方法为支持的 object 成员函数、suspend 函数；
- Action 参数符合首版 event / event + ActionContext 约定；
- 规则返回 `Unit` 或动作结果类型；
- Provider 返回非 Unit 类型；
- Provider key 唯一，限定类型匹配，规则依赖可解析；
- `@FromProvider` 只能用于规则参数，不能与不支持的默认参数/可空参数组合。

实现时优先输出指向具体符号的 KSP 编译错误，不在运行时才暴露普通配置错误。

## 7. 规则顺序约定

| order 范围 | 建议用途 |
| --- | --- |
| 1–4 | 冷却、频率限制 |
| 5–9 | 资格检查、状态检查 |
| 10–49 | 加成和参数计算 |
| 50–99 | 资源扣除或操作准备 |
| 100+ | after 结算、记录 |

数字只是团队约定，不自动赋予事务或回滚语义。消费/扣款类规则必须与后续动作失败时的处理策略一同设计，不能因为 order 较早就认为引擎可以恢复副作用。

## 8. 事务与回滚

### 8.1 第一版不实现自动回滚

自动恢复数据库、内存状态、消息发送、事件广播和后台协程需要不同的边界与补偿策略。当前不把“规则失败后自动逆序回滚”作为框架能力，也不定义 Compensation 接口或回滚范围枚举。

第一版的 `Rollback` 只是待讨论的动作结果候选，不触发任何回滚。实现前必须先确定它的含义、触发阶段和可恢复副作用；在此之前，实际失败仍由业务事务或现有错误处理逻辑负责。

### 8.2 后续回滚候选范围

目前识别到的候选对象：

- 道具消耗；
- 用户/游戏状态变更；
- 资金流动。

这些操作可能有不同的数据一致性和幂等要求，不预设可以用一个通用补偿器解决。优先在数据层明确数据库事务边界；跨内存或外部效果时，再设计显式且可审计的业务补偿。用户提出的“回滚应在最后执行”作为后续语义讨论项，本版不据此推导执行顺序。

## 9. 生命周期与初始化

`economy-main` 在数据库、Manager/服务及业务模块初始化完成后，聚合生成的 `ActionModule` 并创建规则引擎；之后再注册 HuYanAuthorize 事件入口。确切初始化顺序应结合各 Provider 依赖的服务确定。

HuYanAuthorize 仍负责匹配命令和权限。入口只在复杂动作中调用生成 facade；普通动作仍可直接调用业务 object。

最终插件包只包含运行时必需模块。KSP Processor、KSP API 等编译期依赖不得作为插件运行时依赖打包。Hibernate、HikariCP 和 log4j 排除约定保持不变。

## 10. 渐进落地路线

| 阶段 | 内容 | 验收重点 |
| --- | --- | --- |
| P0 | 定义结果语义、Provider 限定/作用域、object 和函数签名约束 | 文档契约无歧义 |
| P1 | 新建运行时模块，手工构造一个 ActionModule 和规则链 | 规则顺序、正常结束、静默结束、文本回复 |
| P2 | 新建 KSP Processor，生成单模块 facade、规则和 Provider 描述 | KSP 生成代码可编译且无反射 |
| P3 | 增加模块级生成与 main 聚合 | core/game/main 模块组合、重复 ID/key 检查 |
| P4 | 选择低副作用复杂流程试点迁移 | 行为与原流程一致，普通动作不受影响 |
| P5 | 按领域逐步迁移确有规则编排收益的复杂动作 | 回归测试覆盖业务结果及权限入口 |
| P6 | 根据真实迁移案例设计回滚能力 | 明确事务、补偿对象、幂等和执行时机后再实施 |

每个阶段独立构建和验证。首个试点应避开长时间交互、后台协程、跨系统副作用较多的流程。签到和钓鱼可待框架契约与模块聚合验证后再评估。

## 11. 测试要求

### 11.1 运行时测试

- before / after 顺序稳定；
- Continue、EndSilently、EndWithMessage 的终止行为正确；
- before 结束时动作本体和 after 不执行；
- action 完成后 after 按序执行；
- action 异常不被结束结果吞掉；
- Provider 按类型、限定名和 Action 作用域解析；
- 一个 Action 的局部 Provider 对其他 Action 不可见；
- 全局与局部 Provider key 冲突会明确失败。

### 11.2 KSP 编译测试

通过 compile-testing 或 Gradle fixture 覆盖成功生成和预期编译失败：重复 actionId、缺少 action、Provider 缺失/歧义/类型不匹配、重复 key、不支持的签名、跨模块聚合冲突。

### 11.3 项目集成验证

沿用当前六模块测试，并额外构建 `buildPlugin`。确认生成代码进入相应业务模块 jar，运行时引擎进入最终插件，KSP Processor 不进入最终插件依赖。

## 12. 已知限制与后续演进

- 规则链不是所有 Action 的统一包装要求。
- 首版不做运行时动态注册。
- 首版不提供自动回滚。
- 首版只支持约定的 object Action 与 event / event + context 签名。
- 同一个 Action 下同类型多值通过显式 Provider 限定名区分。
- 全局 Provider 与 Action 局部 Provider 的冲突采取严格报错，不静默覆盖。
- 多模块聚合必须显式发生在 main；KSP 不跨 Gradle 模块扫描源码。
- `EndWithMessage` 首版只承诺文本回复；复杂消息体留待真实需求出现后设计。

后续可根据迁移案例评估 Provider 多 Action 共享、参数默认值/可空支持、消息内容抽象、事务集成和补偿机制。不要在没有真实用例前扩大首版 API。

## 附录：名词

| 名词 | 含义 |
| --- | --- |
| Action | 可选的规则链业务流程入口 |
| Rule | before/after 流程步骤 |
| Provider | 按类型和限定名装配规则参数的函数 |
| Qualifier | 区分同一类型不同 Provider 的稳定字符串标识 |
| Action-local Provider | 仅对声明绑定的 actionId 可见的 Provider |
| ActionContext | 单次 Action 调用的上下文 |
| ActionModule | 单个 Gradle 模块生成的规则链注册描述 |
| Facade | KSP 生成的 Action 代理入口 |
