package cn.chahuyun.economy.actionchain.annotation

/*
 * ============================================================================
 * Action 规则链注解集合（编译期契约）
 * ============================================================================
 *
 * 这里定义业务模块向规则链框架登记逻辑的唯一方式。整套框架分成两半：
 *
 * 1. 编译期（economy-action-ksp）
 *    - 扫描本文件中的注解，收集 @Action / @Before / @After / @Provider；
 *    - 校验签名、actionId 唯一性、规则引用是否存在、Provider 类型与限定名是否冲突；
 *    - 为每个标注了 @Action 的 object 生成 `<Owner>Actions` 门面；
 *    - 为每个 Gradle 模块生成一个 `<ModuleId>ActionModule` 描述对象。
 *
 * 2. 运行期（economy-action）
 *    - economy-main 把所有模块描述聚合成唯一的 ActionEngine；
 *    - 门面调用 ActionRuntime，最终由 ActionEngine 按 before → action → after 顺序执行。
 *
 * 重要：所有注解都是 `AnnotationRetention.SOURCE`。
 * 编译完成后注解信息不会进入 class 文件，运行期无法通过反射读取，
 * 所以“规则/动作”的接线完全依赖 KSP 生成代码，手写反射查找是行不通的。
 */

/**
 * 声明一个可以经由规则链执行的“动作”（Action）。
 *
 * 被标注的函数会成为该 actionId 的主逻辑：引擎先执行所有 [Before] 规则，
 * 全部返回 [cn.chahuyun.economy.actionchain.runtime.ActionResult.Continue] 后才执行本函数，
 * 随后执行所有 [After] 规则。
 *
 * ## 约束（由 KSP 在编译期校验）
 * - 必须写在 `object` 的成员函数上，顶层函数、扩展函数、普通 class 的成员都不支持；
 * - 必须是 `suspend` 函数：动作内部通常要访问数据库或发送消息；
 * - 可见性至少为 `internal`，不能是 `private` / `protected`（生成的门面需要调用它）；
 * - 参数只支持两种形态：
 *   - `(event: 事件类型)` —— 只接收触发事件；
 *   - `(event: 事件类型, context: ActionContext)` —— 额外拿到本次调用的上下文；
 * - 第一个参数必须是事件对象，不能是 `ActionContext`；
 * - 返回值可以是 `Unit`，也可以是任意值；返回值会存进上下文，供 after 规则通过
 *   [cn.chahuyun.economy.actionchain.runtime.ActionContext.actionResult] 读取。
 *
 * ## 示例
 * ```kotlin
 * object SignUsecase {
 *     @Action("core.sign")
 *     suspend fun sign(event: GroupMessageEvent) { ... }
 * }
 * // 生成门面后由入口调用：
 * // SignUsecaseActions.sign(event)
 * ```
 *
 * @property actionId 动作全局唯一标识，建议使用 `领域.功能` 形式（如 `core.sign`、`game.fish`）。
 *                   同一个模块内重复会被 KSP 报错，跨模块重复会在 ActionEngine 构造时抛异常。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Action(val actionId: String)

/**
 * 声明一条在动作**之前**执行的前置规则。
 *
 * 前置规则适合放横切逻辑：开关校验、冷却判断、权限判断、参数补全、状态恢复等。
 * 引擎按 [order] 升序执行，`order` 相同时按 `ruleId` 字典序排序，保证执行顺序稳定可复现。
 *
 * ## 返回值语义
 * - 返回 `Unit`：等价于 [cn.chahuyun.economy.actionchain.runtime.ActionResult.Continue]，继续下一条规则；
 * - 返回 `ActionResult.Continue`：继续下一条规则；
 * - 返回 `ActionResult.EndSilently`：立即终止本次调用，不执行动作，也不发送任何消息；
 * - 返回 `ActionResult.EndWithMessage`：立即终止本次调用，并把消息交给宿主注册的
 *   [cn.chahuyun.economy.actionchain.runtime.ActionResultHandler] 发送。
 *
 * 一旦某条规则返回非 Continue 的结果，后续 before 规则与动作本体都不会执行，
 * after 规则也不会执行（after 只在动作真正执行后才运行）。
 *
 * ## 参数
 * 规则函数的参数会由生成代码自动注入，可以是：
 * - `ActionContext`：直接拿到上下文（可读写 `attrs`、读取动作返回值）；
 * - 任意不可空的类类型：按类型（可用 [FromProvider] 指定限定名）从 Provider 解析。
 *
 * 函数本身必须是 `suspend`，且写在 `object` 中、可见性不低于 `internal`。
 *
 * @property actionId 该规则所属的动作 id，必须与某个 [Action] 声明的 id 一致。
 * @property order 执行顺序，数值越小越先执行，默认 100。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Before(
    val actionId: String,
    val order: Int = 100,
)

/**
 * 声明一条在动作**之后**执行的后置规则。
 *
 * 后置规则只在动作真正执行完成后运行，适合放结果加工、奖励结算、日志上报、消息补发等逻辑。
 * 排序规则与 [Before] 一致：先按 [order] 升序，再按 `ruleId` 字典序。
 *
 * ## 返回值语义
 * - `Unit` / `ActionResult.Continue`：继续执行下一条 after 规则；
 * - `ActionResult.EndSilently`：终止后续 after 规则，不再发送消息；
 * - `ActionResult.EndWithMessage`：终止后续 after 规则，并发送该消息。
 *
 * 动作本体正常结束时，返回值已经写入上下文，after 规则可以用
 * [cn.chahuyun.economy.actionchain.runtime.ActionContext.actionResult] 读取。
 *
 * @property actionId 该规则所属的动作 id。
 * @property order 执行顺序，数值越小越先执行，默认 100。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class After(
    val actionId: String,
    val order: Int = 100,
)

/**
 * 声明一个“值提供者”（Provider）。
 *
 * Provider 用来把“规则需要的入参”从规则体里解耦出来：规则函数只声明自己需要的类型，
 * 生成代码通过 [cn.chahuyun.economy.actionchain.runtime.ActionContext.provide] 在运行时取值。
 *
 * ## 作用域
 * - [actionId] 为空（默认）：**全局 Provider**，所有动作都可见；
 * - [actionId] 非空：**局部 Provider**，仅对指定动作可见。
 *
 * 同一个动作内，局部 Provider 与全局 Provider 会一起参与匹配；
 * 类型 + 限定名完全相同视为冲突，引擎在构造期直接抛异常（局部不能遮蔽同 key 的全局 Provider）。
 *
 * ## 约束（由 KSP 在编译期校验）
 * - 必须是 `object` 中的 `suspend` 成员函数，可见性不低于 `internal`；
 * - 参数只能是 `()` 或 `(ActionContext)`；
 * - 返回值必须是非空、非 `Unit` 的具体类类型（生成代码要用它做 `actionProvider<T>` 的类型实参）。
 *
 * ## 示例
 * ```kotlin
 * object TransferProviders {
 *     @Provider(qualifier = "from")
 *     suspend fun fromAccount(): Account = ...
 * }
 * ```
 *
 * @property qualifier 限定名。同一类型有多个 Provider 时，规则参数必须用 [FromProvider] 指定限定名，
 *                     否则引擎会因“歧义”报错；只有一个候选时可省略限定名。
 * @property actionId 作用域动作 id；留空表示全局可见。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Provider(
    val qualifier: String,
    val actionId: String = "",
)

/**
 * 为规则函数的参数指定 Provider 限定名。
 *
 * 当同一个类型存在多个 Provider 时，必须用本注解告诉框架“要哪一个”：
 *
 * ```kotlin
 * @Before("bank.transfer")
 * suspend fun checkBalance(
 *     @FromProvider("from") from: Account,
 *     @FromProvider("to") to: Account,
 * ) { ... }
 * ```
 *
 * 只能标注在规则参数上：
 * - 不能标注 `ActionContext` 参数（上下文由框架直接注入，不经过 Provider）；
 * - 不能标注 Provider 函数自身的参数（Provider 的参数只能是 ActionContext）。
 *
 * @property qualifier 目标 Provider 的限定名，需与 [Provider.qualifier] 一致；留空表示“空限定名”。
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class FromProvider(val qualifier: String = "")
