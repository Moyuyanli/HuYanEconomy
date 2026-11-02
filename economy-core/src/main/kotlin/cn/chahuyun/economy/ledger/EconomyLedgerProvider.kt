package cn.chahuyun.economy.ledger

import cn.chahuyun.economy.base.ledger.BaseLedger
import cn.chahuyun.economy.base.ledger.EconomyLedger
import cn.chahuyun.economy.config.EconomyBackend
import cn.chahuyun.economy.config.EconomyConfig
import cn.chahuyun.economy.ledger.EconomyLedgerProvider.current
import cn.chahuyun.economy.ledger.EconomyLedgerProvider.init
import cn.chahuyun.economy.ledger.EconomyLedgerProvider.shutdown
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

    /**
     * 已启动的后端单例。
     *
     * `@Volatile` 只保证可见性：写入发生在 `onEnable`（单线程），其余线程在 `onEnable`
     * 之后才开始调用代理，因此不需要额外加锁。置空只发生在 [shutdown]。
     */
    @Volatile
    private var ledger: EconomyLedger? = null

    /** 当前生效的后端；未初始化时使用会抛错，避免静默走错账本。 */
    val current: EconomyLedger
        get() = ledger ?: error("经济后端尚未初始化，请检查 HuYanEconomy.onEnable 的初始化顺序")

    /**
     * 当前生效的后端，未初始化时为 null。
     *
     * 与 [current] 的区别是**不抛异常**，只给状态展示类逻辑用（例如 `hye economy status`
     * 在启动失败后仍要能打印"未初始化"）。业务读写必须用 [current]。
     */
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

    /**
     * 按配置挑出候选后端，**不做启动与自检**。
     *
     * 三层判定：
     * 1. 配置不是 `MIRAI_ECONOMY` → 直接 `BaseLedger`（绝大多数场景，不触碰上游类型）；
     * 2. 配了 `MIRAI_ECONOMY` 但类探测失败 → 按 `economyBackendFallback` 决定抛错或降级；
     * 3. 类存在 → 尝试构造 `MiraiEconomyLedger`，构造期异常同样走降级开关。
     *
     * 这里只处理"选谁"，启动失败的重试与降级在 [init] 里做——两段分开是为了让
     * "类不存在"和"类存在但 start 失败"给出不同的日志与异常信息。
     */
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
