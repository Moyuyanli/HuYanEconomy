package cn.chahuyun.economy.ledger

import cn.chahuyun.economy.base.ledger.BaseLedger
import cn.chahuyun.economy.base.ledger.EconomyLedger
import cn.chahuyun.economy.config.EconomyBackend
import cn.chahuyun.economy.config.EconomyConfig
import cn.chahuyun.economy.utils.Log

/**
 * 经济后端选择器。
 *
 * 启动时按 `Config.yml` 的 `economyBackend` 决定使用哪个 [EconomyLedger]：
 *
 * 1. `BASE`（默认）：直接使用 [BaseLedger]，不触碰任何上游类型；
 * 2. `MIRAI_ECONOMY`：先探测上游类是否存在，再实例化并做一次功能性自检；
 *    探测或自检失败时按 `economyBackendFallback` 决定"降级为 BASE"还是"启动失败"。
 *
 * 运行期不热切换：切换后端等价于换一套账本，必须先迁移余额并重启插件。
 */
object EconomyLedgerProvider {

    @Volatile
    private var ledger: EconomyLedger? = null

    /** 当前生效的后端；未初始化时使用会抛错，避免静默走错账本。 */
    val current: EconomyLedger
        get() = ledger ?: error("经济后端尚未初始化，请检查 HuYanEconomy.onEnable 的初始化顺序")

    /** 当前生效的后端，未初始化时为 null；供状态查询类逻辑使用。 */
    val currentOrNull: EconomyLedger?
        get() = ledger

    /** 当前后端标识。 */
    val backendId: String
        get() = ledger?.backendId ?: "uninitialized"

    /**
     * 选择并启动经济后端；重复调用返回同一实例。
     *
     * @throws IllegalStateException 选择了不可用的上游后端且未开启降级，或 economy-base 自身启动失败
     */
    @Synchronized
    fun init(): EconomyLedger {
        ledger?.let { return it }

        val fallback = EconomyConfig.economyBackendFallback
        var selected = candidate()

        val started = runCatching { selected.start() }
        if (started.isFailure) {
            val cause = started.exceptionOrNull() ?: IllegalStateException("未知错误")
            if (selected.backendId == BaseLedger.backendId) {
                throw IllegalStateException("economy-base 初始化失败，账户表不可用", cause)
            }
            if (!fallback) {
                throw IllegalStateException("${selected.backendId} 初始化失败，且未开启降级（economyBackendFallback=false）", cause)
            }
            Log.error("${selected.backendId} 初始化失败，已降级为 BASE", cause)
            selected = BaseLedger
            selected.start()
        }

        ledger = selected
        Log.info("经济后端已启用: ${selected.backendId}")
        return selected
    }

    /** 插件关闭时释放后端资源。 */
    @Synchronized
    fun shutdown() {
        ledger?.let { runCatching { it.stop() } }
        ledger = null
    }

    private fun candidate(): EconomyLedger {
        val requested = EconomyConfig.economyBackend
        if (requested != EconomyBackend.MIRAI_ECONOMY) return BaseLedger

        if (!MiraiEconomyProbe.available()) {
            val message = "配置要求 MIRAI_ECONOMY，但运行环境未安装上游插件 mirai-economy-core"
            if (!EconomyConfig.economyBackendFallback) error("$message（economyBackendFallback=false）")
            Log.warning("$message，已降级为 BASE")
            return BaseLedger
        }

        return runCatching { MiraiEconomyLedger() }.getOrElse { cause ->
            val message = "mirai-economy 后端实例化失败: ${cause.message}"
            if (!EconomyConfig.economyBackendFallback) throw IllegalStateException(message, cause)
            Log.error("$message，已降级为 BASE", cause)
            BaseLedger
        }
    }
}
