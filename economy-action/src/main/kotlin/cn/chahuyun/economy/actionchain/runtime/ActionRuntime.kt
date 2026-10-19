package cn.chahuyun.economy.actionchain.runtime

import cn.chahuyun.economy.actionchain.runtime.ActionRuntime.invoke


/**
 * Process-wide handle to the single [ActionEngine] assembled by the host module
 * (`economy-main` aggregates every generated `ActionModule` and installs the engine here).
 *
 * Generated facades call [invoke]; they must not know how the engine was built.
 */
object ActionRuntime {
    @Volatile
    private var engine: ActionEngine? = null

    val isInstalled: Boolean
        get() = engine != null

    fun install(engine: ActionEngine) {
        this.engine = engine
    }

    fun uninstall() {
        engine = null
    }

    suspend fun <T> invoke(
        actionId: String,
        event: Any,
        action: suspend (ActionContext) -> T,
    ): ActionExecution<T> = currentEngine().invokeFromEntry(actionId, event, action)

    private fun currentEngine(): ActionEngine = engine
        ?: throw IllegalStateException(
            "ActionRuntime is not installed. Call ActionRuntime.install(ActionEngine) during plugin startup " +
                "before any generated facade is invoked."
        )
}
