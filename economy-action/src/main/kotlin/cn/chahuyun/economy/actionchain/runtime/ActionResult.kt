package cn.chahuyun.economy.actionchain.runtime

/** Business control flow returned by a before/after rule. */
sealed interface ActionResult {
    data object Continue : ActionResult
    data object EndSilently : ActionResult
    data class EndWithMessage(val message: String) : ActionResult
}

/** Result of one invocation. A stopped-before-action invocation has no action value. */
data class ActionExecution<T>(
    val actionId: String,
    val result: ActionResult,
    val actionExecuted: Boolean,
    val value: T?,
)

/** Adapter implemented by the host application, for example by economy-main for Mirai replies. */
fun interface ActionResultHandler {
    suspend fun handle(context: ActionContext, result: ActionResult.EndWithMessage)
}
