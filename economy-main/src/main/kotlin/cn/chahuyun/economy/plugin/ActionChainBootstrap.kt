package cn.chahuyun.economy.plugin

import cn.chahuyun.economy.actionchain.generated.EconomyCoreActionModule
import cn.chahuyun.economy.actionchain.generated.EconomyGameActionModule
import cn.chahuyun.economy.actionchain.runtime.ActionEngine
import cn.chahuyun.economy.actionchain.runtime.ActionModule
import cn.chahuyun.economy.actionchain.runtime.ActionResultHandler
import cn.chahuyun.economy.actionchain.runtime.ActionRuntime
import cn.chahuyun.economy.utils.Log
import net.mamoe.mirai.event.events.MessageEvent

/**
 * Action 规则链装配入口。
 *
 * 各业务模块（economy-core / economy-game）由 KSP 生成 `<ModuleId>ActionModule`，
 * 这里显式聚合成唯一的 [ActionEngine] 并安装到 [ActionRuntime]，
 * 生成的 facade（如 `SignUsecaseActions`）才能静态调用到引擎。
 *
 * 初始化时机：必须在所有 Manager/service 就绪之后、事件注册之前。
 */
object ActionChainBootstrap {

    /** 显式聚合的模块清单；新增业务模块时在这里登记。 */
    private val modules: List<ActionModule> = listOf(
        EconomyCoreActionModule.module,
        EconomyGameActionModule.module,
    )

    /**
     * 规则返回 [cn.chahuyun.economy.actionchain.runtime.ActionResult.EndWithMessage] 时的回复出口。
     *
     * 首版只处理消息事件；其他事件类型没有统一的消息发送入口，记录告警后丢弃。
     */
    private val resultHandler = ActionResultHandler { context, result ->
        val event = context.event
        if (event is MessageEvent) {
            event.subject.sendMessage(result.message)
        } else {
            Log.warning(
                "规则链: action '${context.actionId}' 返回 EndWithMessage，" +
                    "但事件 ${event::class.simpleName} 不支持直接回复，消息已丢弃"
            )
        }
    }

    /** 唯一运行时引擎；构造时完成 actionId 全局唯一、规则引用、Provider 冲突校验。 */
    val engine: ActionEngine = ActionEngine(modules, resultHandler)

    /** 当前已注册的 actionId，用于启动日志与自检。 */
    val registeredActionIds: Set<String> = modules.flatMap(ActionModule::actionIds).toSet()

    /** 安装引擎；重复调用会以新实例覆盖旧实例。 */
    fun install() {
        ActionRuntime.install(engine)
        val summary = modules.joinToString(", ") { module ->
            "${module.moduleId}[${module.actionIds.sorted().joinToString("/")}]"
        }
        Log.info(
            "Action 规则链装配完成：${modules.size} 个模块，${registeredActionIds.size} 个 action -> $summary；" +
                "规则 ${modules.sumOf { it.rules.size }} 条，Provider ${modules.sumOf { it.providers.size }} 个"
        )
    }

    /** 卸载引擎；插件停用时调用，避免残留指向旧实例的引用。 */
    fun shutdown() {
        if (ActionRuntime.isInstalled) {
            ActionRuntime.uninstall()
        }
    }
}
