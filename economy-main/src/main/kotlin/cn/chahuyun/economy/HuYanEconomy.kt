package cn.chahuyun.economy

import cn.chahuyun.authorize.AuthorizeServer
import cn.chahuyun.authorize.exception.ExceptionHandle
import cn.chahuyun.economy.base.EconomyCore
import cn.chahuyun.economy.command.EconomyCommand
import cn.chahuyun.economy.config.EconomyConfig
import cn.chahuyun.economy.config.EconomyPluginConfig
import cn.chahuyun.economy.config.RobMsgConfig
import cn.chahuyun.economy.constant.Icon
import cn.chahuyun.economy.data.PrizesData
import cn.chahuyun.economy.data.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.feature.bank.BankManager
import cn.chahuyun.economy.feature.privatebank.PrivateBankManager
import cn.chahuyun.economy.feature.redpack.RedPackManager
import cn.chahuyun.economy.feature.title.TitleManager
import cn.chahuyun.economy.game.GameOverviewBridge
import cn.chahuyun.economy.games.farm.FarmCropManager
import cn.chahuyun.economy.games.farm.FarmGameOverviewProvider
import cn.chahuyun.economy.games.lottery.LotteryManager
import cn.chahuyun.economy.manager.FishingManager
import cn.chahuyun.economy.plugin.*
import cn.chahuyun.economy.runtime.EconomyRuntime
import cn.chahuyun.economy.scheduler.HuYanScheduler
import cn.chahuyun.economy.service.EconomyAsyncService
import cn.chahuyun.economy.utils.HibernateUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.extension.PluginComponentStorage
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin

/**
 * 壶言经济插件主类。
 */
object HuYanEconomy : KotlinPlugin(
    JvmPluginDescription(
        id = "cn.chahuyun.HuYanEconomy",
        version = EconomyBuildConstants.VERSION,
        name = "HuYanEconomy"
    ) {
        author("Moyuyanli")
        info("壶言经济-一款娱乐插件")
        dependsOn("xyz.cssxsh.mirai.plugin.mirai-economy-core", ">=1.0.6", false)
        dependsOn("cn.chahuyun.HuYanAuthorize", ">=1.3.6", false)
    }
) {

    /**
     * 插件运行状态。
     */
    @JvmField
    var PLUGIN_STATUS: Boolean = false

    /**
     * 插件配置。
     */
    lateinit var config: EconomyConfig

    /**
     * 抢劫消息配置。
     */
    @JvmField
    var robConfig: RobMsgConfig? = null

    /**
     * 插件所属 bot。
     */
    @JvmField
    var bot: Bot? = null

    override fun PluginComponentStorage.onLoad() {
        // onLoad 阶段 mirai logger 已可用，但配置、数据库和 bot 还未绑定。
        // 这里只做全局日志桥接和运行时对象绑定，避免提前触发业务初始化。
        Log.configure(
            info = { logger.info(it) },
            warning = { logger.warning(it) },
            error = { logger.error(it) },
            errorWithThrowable = { message, throwable -> logger.error(message, throwable) },
            debug = { logger.debug(it) },
        )
        System.setProperty("log4j2.StatusLogger.level", "OFF")
        System.setProperty("org.apache.logging.log4j.simplelog.StatusLogger.level", "OFF")
        PLUGIN_STATUS = true
        EconomyRuntime.bind(HuYanEconomy)
        EconomyRuntime.pluginStatus = true
    }

    override fun onEnable() {
        // onEnable 是完整启动入口：先准备运行时与配置，再初始化数据层和业务模块，最后注册事件。
        EconomyRuntime.bind(this)
        EconomyRuntime.pluginStatus = true
        HuYanScheduler.prepareStartup()
        Icon.init(logger)

        // 加载配置。
        EconomyConfig.reload()
        EconomyPluginConfig.reload()
        RobMsgConfig.reload()
        cn.chahuyun.economy.config.FishingMsgConfig.reload()
        PrizesData.reload()

        // 注册指令。
        CommandManager.registerCommand(EconomyCommand(), false)

        config = EconomyConfig
        robConfig = RobMsgConfig
        EconomyAsyncService.init()
        GameOverviewBridge.register(FarmGameOverviewProvider)

        // 初始化插件功能。
        MessageUtil.init(this)
        PluginManager.init()

        // 注册插件权限 code。
        PermCodeManager.init(this)

        // 初始化插件数据库。
        HibernateUtil.init(this)
        EconomyCore.init()

        // 初始化实体代理框架：先把配置中的版本策略注入 data 层，再注册各模块代理。
        DataSourceStrategyImpl.configurePersistence(EconomyPluginConfig.entityDataVersions) {
            EconomyPluginConfig.entityDataVersions = it
        }
        EntityProxyRegistry.init()
        Log.info(formatEntityVersionSummary())

        // 加载功能模块。
        // 经济后端选择：默认 economy-base；只有配置 MIRAI_ECONOMY 且上游插件可用时才切到
        // mirai-economy，探测失败按 economyBackendFallback 决定降级或启动失败。
        cn.chahuyun.economy.ledger.EconomyLedgerProvider.init()
        LotteryManager.init()
        FarmCropManager.init()
        BankManager.init()
        RedPackManager.init()
        TitleManager.init()
        PrivateBankManager.init()
        YiYanManager.init()
        FishingManager.init()
        FactorManager.init()
        PluginPropsManager.init()
        HuYanScheduler.registerSubmittedTasks()

        // 自定义称号模板依赖 TitleTemplateManager 初始化完成后再加载。
        TitleTemplateManager.loadingCustomTitle()

        // 装配 Action 规则链：聚合各模块生成描述并安装唯一引擎。
        // 必须早于事件注册，否则入口调用生成 facade 时运行时会抛“未安装引擎”。
        ActionChainBootstrap.install()

        // 注册消息事件，统一扫描 action 包。
        AuthorizeServer.registerEvents(
            plugin = this,
            packageName = "cn.chahuyun.economy.action",
            exceptionHandle = ExceptionHandle(),
            prefix = EconomyConfig.prefix,
            useKsp = true
        )

        // 已迁入 feature/ 的功能（如签到）不再注册自定义事件：
        // 其前置/后置扩展点由规则链的 @Before / @After 承载，随 ActionChainBootstrap 一起装配。
        // 这里只保留尚未迁移功能的 Mirai 事件订阅。

        EconomyPluginConfig.firstStart = false
        Log.info("HuYanEconomy 已加载！当前版本 ${description.version} !")
    }

    private fun formatEntityVersionSummary(): String {
        val versions = EntityProxyRegistry.currentVersions()
        val v2Modules = versions
            .filterValues { it.name == "V2" }
            .keys
            .sorted()

        if (v2Modules.isEmpty()) {
            return "实体数据源版本加载完成：全部 ${versions.size} 个模块使用 V1(默认)"
        }

        val v1Count = versions.size - v2Modules.size
        return "实体数据源版本加载完成：V2 ${v2Modules.size} 个[${v2Modules.joinToString(", ")}]，V1(默认) ${v1Count} 个"
    }

    override fun onDisable() {
        // 停机按“停止新任务 -> 关闭长生命周期模块”的方向收敛，避免卸载后仍有定时任务访问插件状态。
        PLUGIN_STATUS = false
        EconomyRuntime.pluginStatus = false

        LotteryManager.close()
        FishingManager.shutdown()
        YiYanManager.shutdown()
        cn.chahuyun.economy.ledger.EconomyLedgerProvider.shutdown()
        HuYanScheduler.stop()
        EconomyAsyncService.shutdown()
        ActionChainBootstrap.shutdown()
        Log.info("插件已卸载")
    }
}
