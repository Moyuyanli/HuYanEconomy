# Action 骨架契约（阶段一产物）

> 状态：阶段一契约已落地，阶段二运行时已按第 6 节决议实现并通过测试；KSP 生成见开发手册阶段三。  
> 日期：2026-10-13（阶段二实现完成）

本文把 Action 规则链第一阶段已达成的结构性决定整理成运行时与 KSP 实现的共同输入。注解定义、两个 Gradle 模块骨架和阶段二运行时（`ActionEngine` 等）已存在；KSP 生成的 facade/模块描述见开发手册阶段三。

## 1. 模块边界

```text
economy-core ──> economy-action <── economy-game
      |                                  |
      +---------- generated metadata ----+
                         |
                         v
                  economy-main

economy-action-ksp: KSP 编译期处理器，应用于定义 Action/Rule/Provider 的业务模块
```

- `economy-action`：框架注解和运行时 API；不依赖 core、game、main 或 HuYanAuthorize。
- `economy-action-ksp`：KSP Processor；可依赖注解/API 和 KSP API，但仅作为编译期处理器，不进入插件运行时。
- `economy-core`、`economy-game`：依赖运行时 API，并在有本模块 Action 源码时应用 Processor；各自产生模块级描述和 facade。
- `economy-main`：显式聚合业务模块生成的描述，负责在入口开始接收事件前初始化引擎；继续用 HuYanAuthorize 做命令、权限和事件注册。
- Gradle 依赖不得形成 `economy-action -> economy-core/game` 的反向边。当前 Kotlin `1.9.20`、KSP `1.9.20-1.0.14`、JVM 11 保持不变。
- 规则链注解仅供 KSP 编译期读取，注解草案使用 `SOURCE` retention，避免将运行时反射误作注册机制。

第一阶段代码范围限于注册模块和注解定义：`economy-action` 提供源码注解，`economy-action-ksp` 提供处理器工程壳。此阶段不实现执行引擎、ActionContext、结果类型、Provider 加载器或 KSP Processor。

注解 API 草案固定目标和字段形态如下；包名及结果类型名称留待实现前确认：

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Action(val actionId: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Before(val actionId: String, val order: Int = 100)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class After(val actionId: String, val order: Int = 100)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Provider(val qualifier: String, val actionId: String = "")

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class FromProvider(val qualifier: String = "")
```

空 `Provider.actionId` 表示全局；非空表示仅对该 actionId 可见。空 qualifier 表示未限定，只能在有效范围内类型唯一时解析。

## 2. Action 形态

### 2.1 标注规则

只有具有多步流程、可插拔规则或统一前后置处理价值的业务入口标记 `@Action`。查询、帮助、管理开关和直接委托类动作不强制标记，也不生成 facade。

### 2.2 实例与签名

- 业务实现采用 Kotlin `object`，生成代码直接调用对象，不构造业务实例。
- 常见 Action 方法接收一个 Mirai event。
- 少数需要流程上下文的 Action 可声明 `event, ActionContext`；context 由引擎创建并由生成代码传入。
- 生成 facade 的公开入口只要求调用方提供 event，不要求入口创建或传递 context。
- 仅支持 `suspend` 成员方法；首版不支持构造注入、任意 class 实例、顶层函数或任意多参数方法。
- Action 本体的非 Unit 返回值及其供 After 使用的方式仍未决定，见第 6 节；实现前不得自行设计隐式注入。

调用形态示意（不是可编译 API）：

```kotlin
@MessageAuthorize(...)
suspend fun transfer(event: GroupMessageEvent) {
    TransferUsecaseActions.transfer(event)
}
```

业务模块中 Action、Rule、Provider 均放在 `object` 成员函数上。Rule 可声明 `ActionContext` 参数和 Provider 参数；原始 event 从 context 访问，不作为隐式的第二套参数注入规则。Provider 函数首版形态固定为 `suspend fun provider(ctx: ActionContext): T`，其他输入由 context 提供。

## 3. 规则和结果

### 3.1 规则关联

- `@Before(actionId, order)` 和 `@After(actionId, order)` 通过稳定的 actionId 绑定。
- 同一 action 的规则按 order 升序执行。
- 相同 order 的次序必须稳定，但排序次级键尚未决定；KSP 实现前需确定并测试。
- 规则函数使用 `suspend`；返回 `Unit` 可映射为继续，或显式返回规则结果。

### 3.2 首版结果集合

规则的业务控制结果为：

```text
Continue
EndSilently
EndWithMessage(message)
```

语义：Continue 运行下一步骤；EndSilently 正常结束本次 Action；EndWithMessage 正常结束并产生一条待发送的文本回复。意外错误仍通过异常表达。

回滚/补偿不进入首版 API：不定义 `Rollback` 可执行行为，不定义 Compensation，不承诺对道具、状态或资金副作用自动恢复。待回滚对象、触发阶段和“最后执行”的具体含义另行设计。

执行顺序基线：

| 位置 | 返回 Continue | 返回结束结果 |
| --- | --- | --- |
| Before | 执行下一条 Before | 不执行其余 Before、Action 或 After |
| Action | Action 本体正常完成后进入 After | Action 本体返回语义待确认；当前规则结果不由 Action 本体产生 |
| After | 执行下一条 After | 停止余下 After |

After 产生的 EndWithMessage 与 Action 本体已有消息之间的优先级/去重策略仍待确认。`EndWithMessage` 最终由哪个入口适配 API 发送也待确认；不得让运行时模块直接假设它能向 Mirai Contact 发消息。

## 4. Provider 契约

### 4.1 作用域与 key

Provider 可全局注册，也可绑定单个 actionId。局部 Provider 仅对指定 Action 可见。有效查找键为：

```text
(actionId, valueType, qualifier)
```

全局 Provider 对所有 actionId 可见；某 Action 的局部 Provider 不参与其他 Action 的解析。全局和局部 Provider 在同一个 Action 的有效集合内若出现相同类型与 qualifier，严格报冲突，不静默覆盖。

### 4.2 类型限定

类型相同但来源不同的参数必须使用 qualifier 明确选择。未标 qualifier 的参数只允许在该 Action 的有效 Provider 中恰有一个同类型候选时解析；无候选或多候选分别报告缺失或歧义。参数名不参与匹配。

转账示意：

```kotlin
@Provider(qualifier = "fromUser", actionId = "bank.transfer")
suspend fun fromAccount(ctx: ActionContext): EconomyAccount = ...

@Provider(qualifier = "toUser", actionId = "bank.transfer")
suspend fun toAccount(ctx: ActionContext): EconomyAccount = ...

@Before(actionId = "bank.transfer")
suspend fun validate(
    @FromProvider("fromUser") from: EconomyAccount,
    @FromProvider("toUser") to: EconomyAccount,
) { ... }
```

这里 `fromUser` 和 `toUser` 是稳定 qualifier；不是 Kotlin 参数名。最终注解属性命名尚属 API 草案，但上述查找语义已确定。

### 4.3 Provider 输入

Provider 不限于 userId，可从一次调用的 `ActionContext` 读取事件和上下文数据。首版 Provider 只接收 `ActionContext`；同一 Provider 在一次调用中是否缓存一次仍待确认，阶段二实现前须明确。

## 5. KSP 与多模块契约

- Processor 只扫描应用它的当前 Gradle 模块，不假定跨模块源码扫描。
- 每个业务模块生成自己的 ActionModule 描述、规则绑定、Provider 元数据/调用和 facade。
- `economy-main` 显式聚合各业务模块描述；全局 actionId 冲突和组合后的 Provider 冲突必须检查。
- Processor 生成普通静态 Kotlin 调用代码，不使用反射、方法名字符串分派或无参构造 Action 类。
- 模块间 Provider 元数据能否通过 KSP 在消费者编译期读取，尚未由当前项目构建验证；阶段四必须用当前 KSP 版本实测。若不可行，首版限制跨模块引用或在聚合阶段校验，不以反射兜底。

## 6. 阶段一待确认项与后续门槛

阶段二开始前对以下事项作出决议：

| 待确认项 | 最迟确认阶段 | 原因 |
| --- | --- | --- |
| Provider 限定注解最终名称及属性语法 | 阶段三 | 影响源代码 API；语义已按 qualifier 明确 |
| 相同 order 的稳定次级排序键 | 阶段三 | 影响可重复生成和运行结果 |
| 跨模块 Provider 元数据的编译期可见性 | 阶段四 | 受 KSP/Gradle 实际行为约束 |

阶段二决议（2026-10-13）：

- 规则控制结果命名为 `ActionResult`，包含 Continue、EndSilently、EndWithMessage；Rollback 不进入首版 API。
- Action 本体允许返回任意值。`ActionExecution<T>` 表示规则结果、Action 是否执行及业务返回值；before 提前结束时 `actionExecuted` 为 false 且没有业务返回值。Action 返回值保存到 context，供 After 规则读取。
- `EndWithMessage` 由运行时 `ActionResultHandler` 端口交给上层处理，Mirai 发送适配放在上层。一次 invocation 最多调用一次 handler；框架不检查或去重 Action 本体直接发送的消息，使用规则结果回复的 Action 应避免重复发送同一逻辑消息。
- Provider 首版仅接收 `ActionContext`；同一 invocation 内首次解析后缓存，之后相同 `(类型, qualifier)` 复用该值。
- 同 order 规则按稳定 `ruleId` 字典序排序；KSP 从声明符号生成 id，阶段三验证唯一性与稳定性。

运行时 `ActionModule` 显式声明本模块的 actionId、规则和 Provider。入口 facade 将 Action 本体作为 block 交给 engine；运行时不使用反射查找业务方法。main 聚合模块描述并创建 engine。

## 7. 骨架评审示例

### 简单的单事件 Action

```kotlin
object UserQueryUsecase {
    @Action("user.query")
    suspend fun query(event: MessageEvent) {
        // 查询动作
    }
}
```

规则链入口由生成 facade 提供；没有需要编排的规则时，不应仅为代理而标记 Action。

### 同类型双 Provider 的转账 Action

```kotlin
object TransferUsecase {
    @Action("bank.transfer")
    suspend fun transfer(event: MessageEvent, ctx: ActionContext) {
        // 转账动作；付款方和收款方由规则 Provider 分别装配
    }
}
```

`EconomyAccount` 的两个值分别由 `fromUser`、`toUser` Provider 提供，规则参数显式指定 qualifier。该例验证“同类型不同来源”和“局部 Provider 只对转账可见”，不是对账户查询/资金事务实现的规定。

## 8. 阶段一验收记录

- [x] 确认运行时模块与 Processor 模块的职责及依赖方向，并在 settings 中注册两个模块。
- [x] 在 `economy-action` 中定义 `SOURCE` retention 的 Action/Rule/Provider 注解骨架。
- [x] 建立只含编译期依赖的 `economy-action-ksp` 工程壳，未注册空 Processor。
- [x] 确认 object Action、event 主签名、可选 context 注入和 facade 对入口的边界。
- [x] 确认只有复杂流程标注 Action。
- [x] 确认三类首版规则结果，回滚明确排除首版实现。
- [x] 确认全局/Action 局部 Provider、类型+qualifier key、歧义与冲突规则。
- [x] 提供简单事件 Action 和同类型双账户转账两个契约示例。
- [x] 将尚未确定的实现/API 决策列出并绑定最迟确认阶段。
- [x] 完成阶段二运行时结果、业务返回值、回复处理和 Provider 缓存策略决议。

结论：阶段一的结构设计与注解/Gradle 骨架交付完成；进入阶段二前，须先决议第 6 节标明的阶段二门槛事项。
