package cn.chahuyun.economy

import cn.chahuyun.authorize.AuthorizeServer
import cn.chahuyun.authorize.exception.ExceptionHandle
import cn.chahuyun.economy.action.GamesAction
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
import cn.chahuyun.economy.manager.BankManager
import cn.chahuyun.economy.manager.LotteryManager
import cn.chahuyun.economy.manager.PrivateBankManager
import cn.chahuyun.economy.manager.TitleManager
import cn.chahuyun.economy.plugin.*
import cn.chahuyun.economy.scheduler.HuYanScheduler
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
 * 澹惰█缁忔祹鎻掍欢涓荤被 (Kotlin Object)
 */
object HuYanEconomy : KotlinPlugin(
    JvmPluginDescription(
        id = "cn.chahuyun.HuYanEconomy",
        version = EconomyBuildConstants.VERSION,
        name = "HuYanEconomy"
    ) {
        author("Moyuyanli")
        info("澹惰█缁忔祹")
        dependsOn("xyz.cssxsh.mirai.plugin.mirai-economy-core", ">=1.0.6", false)
        dependsOn("cn.chahuyun.HuYanAuthorize", ">=1.3.6", false)
    }
) {

    /**
     * 鎻掍欢杩愯鐘舵€?
     */
    @JvmField
    var PLUGIN_STATUS: Boolean = false

    /**
     * 閰嶇疆
     */
    lateinit var config: EconomyConfig

    /**
     * 閽撻奔娑堟伅閰嶇疆
     */
    @JvmField
    var msgConfig: FishingMsgConfig? = null

    /**
     * 鎶㈠姭娑堟伅閰嶇疆
     */
    @JvmField
    var robConfig: RobMsgConfig? = null

    /**
     * 鎻掍欢鎵€灞瀊ot
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
    }

    override fun onEnable() {
        HuYanScheduler.prepareStartup()
        Icon.init(logger)

        // 鍔犺浇閰嶇疆
        EconomyConfig.reload()
        EconomyPluginConfig.reload()
        FishingMsgConfig.reload()
        RobMsgConfig.reload()
        PrizesData.reload()

        // 娉ㄥ唽鎸囦护
        CommandManager.registerCommand(EconomyCommand(), false)

        config = EconomyConfig
        msgConfig = FishingMsgConfig
        robConfig = RobMsgConfig

        // 鎻掍欢鍔熻兘鍒濆鍖?
        MessageUtil.init(this)
        PluginManager.init()

        // 鎻掍欢鏉冮檺code娉ㄥ唽
        PermCodeManager.init(this)

        // 鍒濆鍖栨彃浠舵暟鎹簱
        HibernateUtil.init(this)

        // Initialize entity data proxy framework.
        DataSourceStrategyImpl.configurePersistence(EconomyPluginConfig.entityDataVersions) {
            EconomyPluginConfig.entityDataVersions = it
        }
        EntityProxyRegistry.init()
        Log.info(formatEntityVersionSummary())

        // 鍔熻兘鍔犺浇
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

        // 娉ㄥ唽娑堟伅
        // 娉ㄥ唽娑堟伅锛堢粺涓€鎵弿 action 鍖咃級
        AuthorizeServer.registerEvents(
            plugin = this,
            packageName = "cn.chahuyun.economy.action",
            exceptionHandle = ExceptionHandle(),
            prefix = EconomyConfig.prefix,
            useKsp = true
        )

        val eventChannel = GlobalEventChannel.parentScope(this)

        // 鐩戝惉鑷畾涔夌鍒颁簨浠?
        eventChannel.subscribeAlways<SignRewardEvent>(priority = EventPriority.HIGH) { cn.chahuyun.economy.manager.SignManager.randomSignGold(it) }
        eventChannel.subscribeAlways<SignRewardEvent> { cn.chahuyun.economy.manager.SignManager.signProp(it) }
        eventChannel.subscribeAlways<FishStartEvent> { GamesAction.fishStart(it) }
        eventChannel.subscribeAlways<FishRollEvent> { GamesAction.fishRoll(it) }

        Log.info("浜嬩欢宸茬洃鍚?")

        EconomyPluginConfig.firstStart = false
        Log.info("HuYanEconomy宸插姞杞斤紒褰撳墠鐗堟湰 ${description.version} !")
    }

    private fun formatEntityVersionSummary(): String {
        val versions = EntityProxyRegistry.currentVersions()
        val v2Modules = versions
            .filterValues { it.name == "V2" }
            .keys
            .sorted()

        if (v2Modules.isEmpty()) {
            return "瀹炰綋鏁版嵁婧愮増鏈姞杞藉畬鎴愶細鍏ㄩ儴 ${versions.size} 涓ā鍧椾娇鐢?V1(榛樿)"
        }

        val v1Count = versions.size - v2Modules.size
        return "实体数据源版本加载完成：V2 ${v2Modules.size} 个[${v2Modules.joinToString(", ")}]，V1(默认) ${v1Count} 个"
    }

    override fun onDisable() {
        PLUGIN_STATUS = false

        cn.chahuyun.economy.manager.GamesManager.shutdown()
        LotteryManager.close()
        YiYanManager.shutdown()
        HuYanScheduler.stop()
        Log.info("鎻掍欢宸插嵏杞?")
    }
}

