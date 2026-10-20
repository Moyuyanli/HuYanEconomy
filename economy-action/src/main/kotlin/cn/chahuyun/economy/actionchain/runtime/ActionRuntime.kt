package cn.chahuyun.economy.actionchain.runtime

import cn.chahuyun.economy.actionchain.runtime.ActionRuntime.engine
import cn.chahuyun.economy.actionchain.runtime.ActionRuntime.install
import cn.chahuyun.economy.actionchain.runtime.ActionRuntime.invoke
import cn.chahuyun.economy.actionchain.runtime.ActionRuntime.uninstall


/**
 * 进程级的 [ActionEngine] 持有者，是“生成代码”与“宿主装配”之间的唯一接缝。
 *
 * ## 为什么要这个对象
 * KSP 生成的 facade（`<Owner>Actions`）必须调用引擎，但它**不应该知道**引擎是怎么装配的：
 * 引擎需要聚合 core / game 的所有模块，而 facade 位于业务模块内部，二者互相依赖会形成环。
 * 于是这里提供一个全局单例入口：
 *
 * - 宿主（`economy-main` 的 `ActionChainBootstrap`）负责聚合模块、构造引擎，然后 [install]；
 * - 生成代码只调用 [invoke]，完全不关心模块清单与构造参数。
 *
 * ## 生命周期
 * - 必须在插件启动、事件注册**之前** [install]，否则指令一触发就会抛“未安装引擎”；
 * - 插件停用时 [uninstall]，避免热重载后还持有旧引擎（旧引擎里的规则可能引用已卸载的类/状态）。
 *
 * ## 线程安全
 * [engine] 用 `@Volatile` 修饰，保证 install/uninstall 与并发的 [invoke] 之间可见性正确。
 * 引擎本身是不可变对象，可以安全地被多个协程并发调用。
 */
object ActionRuntime {

    /**
     * 当前已安装的引擎；未安装时为 `null`。
     *
     * `@Volatile` 保证写入对其它线程立即可见（插件启停与消息处理通常在不同线程）。
     */
    @Volatile
    private var engine: ActionEngine? = null

    /** 引擎是否已安装。可用于启动自检或测试断言。 */
    val isInstalled: Boolean
        get() = engine != null

    /**
     * 安装（或替换）全局引擎。
     *
     * 通常在宿主启动流程中调用一次；重复调用会用新引擎覆盖旧引擎。
     *
     * @param engine 已聚合好全部模块的引擎实例。
     */
    fun install(engine: ActionEngine) {
        this.engine = engine
    }

    /**
     * 卸载全局引擎。
     *
     * 卸载后任何 facade 调用都会抛 [IllegalStateException]，
     * 因此只应在插件停用、确认不会再有事件进来时调用。
     */
    fun uninstall() {
        engine = null
    }

    /**
     * 发起一次规则链调用（供生成的 facade 使用）。
     *
     * 本方法只是“取引擎 + 转发”的一层薄封装，真正的执行语义见 [ActionEngine.invokeFromEntry]。
     *
     * @param T 动作返回值类型。
     * @param actionId 动作 id，必须已注册。
     * @param event 触发事件，原样交给引擎。
     * @param action 动作本体（facade 传入）。
     * @return 本次调用结果。
     * @throws IllegalStateException 引擎尚未安装时抛出，说明启动顺序有误。
     */
    suspend fun <T> invoke(
        actionId: String,
        event: Any,
        action: suspend (ActionContext) -> T,
    ): ActionExecution<T> = currentEngine().invokeFromEntry(actionId, event, action)

    /**
     * 取出当前引擎；未安装时给出可操作的错误信息。
     *
     * 错误信息特意写了修复方式（在启动期 install），避免上线后只看到一句无上下文的 NPE。
     */
    private fun currentEngine(): ActionEngine = engine
        ?: throw IllegalStateException(
            "ActionRuntime is not installed. Call ActionRuntime.install(ActionEngine) during plugin startup " +
                "before any generated facade is invoked."
        )
}
