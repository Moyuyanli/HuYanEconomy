package cn.chahuyun.economy

import cn.chahuyun.authorize.AuthorizeServer
import cn.chahuyun.authorize.exception.ExceptionHandle
import cn.chahuyun.economy.command.EconomyCommand
import cn.chahuyun.economy.config.EconomyConfig
import cn.chahuyun.economy.config.EconomyPluginConfig
import cn.chahuyun.economy.config.FishingMsgConfig
import cn.chahuyun.economy.config.RobMsgConfig
import cn.chahuyun.economy.constant.Icon
import cn.chahuyun.economy.data.PrizesData
import cn.chahuyun.economy.data.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.fish.FishRollEvent
import cn.chahuyun.economy.fish.FishStartEvent
import cn.chahuyun.economy.game.DefaultGameOverviewProvider
import cn.chahuyun.economy.game.GameOverviewBridge
import cn.chahuyun.economy.manager.BankManager
import cn.chahuyun.economy.manager.LotteryManager
import cn.chahuyun.economy.manager.PrivateBankManager
import cn.chahuyun.economy.manager.TitleManager
import cn.chahuyun.economy.plugin.*
import cn.chahuyun.economy.runtime.EconomyRuntime
import cn.chahuyun.economy.scheduler.HuYanScheduler
import cn.chahuyun.economy.service.GameEventService
import cn.chahuyun.economy.sign.SignRewardEvent
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.HibernateUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.extension.PluginComponentStorage
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.GlobalEventChannel

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
     * 钓鱼消息配置。
     */
    @JvmField
    var msgConfig: FishingMsgConfig? = null

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
        EconomyRuntime.bind(this)
        EconomyRuntime.pluginStatus = true
        HuYanScheduler.prepareStartup()
        Icon.init(logger)

        // 加载配置。
        EconomyConfig.reload()
        EconomyPluginConfig.reload()
        FishingMsgConfig.reload()
        RobMsgConfig.reload()
        PrizesData.reload()

        // 注册指令。
        CommandManager.registerCommand(EconomyCommand(), false)

        config = EconomyConfig
        msgConfig = FishingMsgConfig
        robConfig = RobMsgConfig
        GameOverviewBridge.register(DefaultGameOverviewProvider)

        // 初始化插件功能。
        MessageUtil.init(this)
        PluginManager.init()

        // 注册插件权限 code。
        PermCodeManager.init(this)

        // 初始化插件数据库。
        HibernateUtil.init(this)

        // Initialize entity data proxy framework.
        DataSourceStrategyImpl.configurePersistence(EconomyPluginConfig.entityDataVersions) {
            EconomyPluginConfig.entityDataVersions = it
        }
        EntityProxyRegistry.init()
        Log.info(formatEntityVersionSummary())

        // 加载功能模块。
        EconomyUtil.init()
        LotteryManager.init()
        FishManager.init()
        FarmCropManager.init()
        BankManager.init()
        TitleManager.init()
        PrivateBankManager.init()
        YiYanManager.init()
        cn.chahuyun.economy.manager.GamesManager.init()
        cn.chahuyun.economy.manager.FarmManager.init()
        FactorManager.init()
        PluginPropsManager.init()
        HuYanScheduler.registerSubmittedTasks()

        // Load custom title templates.
        TitleTemplateManager.loadingCustomTitle()

        // 注册消息事件，统一扫描 action 包。
        AuthorizeServer.registerEvents(
            plugin = this,
            packageName = "cn.chahuyun.economy.action",
            exceptionHandle = ExceptionHandle(),
            prefix = EconomyConfig.prefix,
            useKsp = true
        )

        val eventChannel = GlobalEventChannel.parentScope(this)

        // 监听自定义事件。
        eventChannel.subscribeAlways<SignRewardEvent>(priority = EventPriority.HIGH) { cn.chahuyun.economy.manager.SignManager.randomSignGold(it) }
        eventChannel.subscribeAlways<SignRewardEvent> { cn.chahuyun.economy.manager.SignManager.signProp(it) }
        eventChannel.subscribeAlways<FishStartEvent> { GameEventService.handleFishStart(it) }
        eventChannel.subscribeAlways<FishRollEvent> { GameEventService.handleFishRoll(it) }

        Log.info("事件已监听")

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
        PLUGIN_STATUS = false
        EconomyRuntime.pluginStatus = false

        cn.chahuyun.economy.manager.GamesManager.shutdown()
        LotteryManager.close()
        YiYanManager.shutdown()
        HuYanScheduler.stop()
        Log.info("插件已卸载")
    }
}
