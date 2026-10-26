package cn.chahuyun.economy.actionchain.runtime

/**
 * before / after 规则返回的“业务控制流”。
 *
 * 规则用返回 [ActionResult] 来告诉引擎下一步该怎么走：继续、静默结束，还是结束并回复。
 * 只有这三个语义，框架**不做自动回滚**：需要补偿时由业务规则自己写反向逻辑。
 *
 * 规则函数也可以直接返回 `Unit`，此时 KSP 生成的代码会把它换算成 [Continue]，
 * 因此绝大多数“只做校验、不打断流程”的规则都写 `Unit` 即可。
 */
sealed interface ActionResult {

    /**
     * 继续执行：before 阶段继续下一条前置规则，after 阶段继续下一条后置规则。
     *
     * 这是唯一的“放行”结果；只要某条规则返回非 [Continue] 的结果，本次调用就会被截断。
     */
    data object Continue : ActionResult

    /**
     * 静默结束：终止本次调用，不执行后续规则/动作，也**不发送任何消息**。
     *
     * 适合“玩家处于冷却中但已经由其它渠道提示过”“内部状态不允许，但不需要打扰用户”等场景。
     */
    data object EndSilently : ActionResult

    /**
     * 结束并回复：终止本次调用，并把 [message] 交给宿主注册的 [ActionResultHandler] 发送。
     *
     * 首版只支持纯文本；需要发图片/消息链时，由宿主在 [ActionResultHandler] 里自行扩展，
     * 规则侧不要依赖具体发送实现。
     *
     * @property message 要发送给用户的文本内容。
     */
    data class EndWithMessage(val message: String) : ActionResult
}

/**
 * 一次规则链调用的完整结果。
 *
 * 引擎在 before 阶段被提前终止时，动作没有执行，此时 [actionExecuted] 为 `false`、[value] 为 `null`；
 * 只有动作真正执行过，[value] 才有值（动作返回 `Unit` 时它是一个 `Unit` 实例，而不是 `null`）。
 *
 * @param T 动作本体的返回值类型；动作为 `Unit` 时通常用 `ActionExecution<Unit>`。
 * @property actionId 本次调用的动作 id。
 * @property result 最终控制流结果：动作正常执行完且 after 没有打断时是 [ActionResult.Continue]，
 *                  否则是被拦截/打断时规则返回的结果。
 * @property actionExecuted 动作本体是否真的执行过。入口可以用它决定是否继续走旧逻辑。
 * @property value 动作本体的返回值；未执行时为 `null`。
 */
data class ActionExecution<T>(
    val actionId: String,
    val result: ActionResult,
    val actionExecuted: Boolean,
    val value: T?,
)

/**
 * `ActionResult.EndWithMessage` 的发送适配器，由宿主应用实现。
 *
 * 规则链运行时本身不依赖 Mirai，也不持有 bot / 群 / 用户对象，所以“怎么发消息”必须交给上层：
 * 例如 economy-main 的实现会判断 `context.event` 是否为 `MessageEvent`，是则直接
 * `event.subject.sendMessage(result.message)`。
 *
 * 约定：引擎在一次调用中最多调用一次本方法（只有最终结果确实是 `EndWithMessage` 时才调用），
 * 实现方不需要做幂等去重。
 */
fun interface ActionResultHandler {

    /**
     * 处理“结束并回复”。
     *
     * @param context 本次调用的上下文，可从中取出 `actionId`、触发事件与业务自定义属性。
     * @param result 规则返回的结束结果，其中 `message` 为要发送的文本。
     */
    suspend fun handle(context: ActionContext, result: ActionResult.EndWithMessage)
}
