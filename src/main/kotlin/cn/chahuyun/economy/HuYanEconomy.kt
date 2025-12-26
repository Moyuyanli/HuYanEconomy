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
import cn.chahuyun.economy.fish.FishRollEvent
import cn.chahuyun.economy.fish.FishStartEvent
import cn.chahuyun.economy.manager.*
import cn.chahuyun.economy.plugin.*
import cn.chahuyun.economy.sign.SignEvent
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.HibernateUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.hutool.cron.CronUtil
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.extension.PluginComponentStorage
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.GlobalEventChannel


/**
 * 壶言经济插件主类 (Kotlin Object)
 */
object HuYanEconomy : KotlinPlugin(
    JvmPluginDescription(
        id = "cn.chahuyun.HuYanEconomy",
        version = EconomyBuildConstants.VERSION,
        name = "HuYanEconomy"
    ) {
        author("Moyuyanli")
        info("壶言经济")
        dependsOn("xyz.cssxsh.mirai.plugin.mirai-economy-core", ">=1.0.6", false)
        dependsOn("cn.chahuyun.HuYanAuthorize", ">=1.3.6", false)
    }
) {

    /**
     * 插件运行状态
     */
    @JvmField
    var PLUGIN_STATUS: Boolean = false

    /**
     * 配置
     */
    lateinit var config: EconomyConfig

    /**
     * 钓鱼消息配置
     */
    @JvmField
    var msgConfig: FishingMsgConfig? = null

    /**
     * 抢劫消息配置
     */
    @JvmField
    var robConfig: RobMsgConfig? = null

    /**
     * 插件所属bot
     */
    @JvmField
    var bot: Bot? = null

    override fun PluginComponentStorage.onLoad() {
        PLUGIN_STATUS = true
    }

    override fun onEnable() {
        Icon.init(logger)

        // 加载配置
        EconomyConfig.reload()
        EconomyPluginConfig.reload()
        FishingMsgConfig.reload()
        RobMsgConfig.reload()
        PrizesData.reload()

        // 注册指令
        CommandManager.registerCommand(EconomyCommand(), false)

        config = EconomyConfig
        msgConfig = FishingMsgConfig
        robConfig = RobMsgConfig

        // 插件功能初始化
        MessageUtil.init(this)
        PluginManager.init()

        // 插件权限code注册
        PermCodeManager.init(this)

        // 初始化插件数据库
        HibernateUtil.init(this)

        // 功能加载
        EconomyUtil.init()
        LotteryManager.init()
        FishManager.init()
        BankManager.init()
        TitleManager.init()
        YiYanManager.init()
        GamesManager.init()
        FactorManager.init()
        PluginPropsManager.init()

        // 注册自定义称号
        TitleTemplateManager.loadingCustomTitle()

        // 注册消息
        AuthorizeServer.registerEvents(
            plugin = this,
            packageName = "cn.chahuyun.economy.manager",
            exceptionHandle = ExceptionHandle(),
            prefix = EconomyConfig.prefix,
            useKsp = true
        )

        val eventChannel = GlobalEventChannel.parentScope(this)

        // 监听自定义签到事件
        eventChannel.subscribeAlways<SignEvent>(priority = EventPriority.HIGH) {
            SignManager.randomSignGold(it)
        }

        eventChannel.subscribeAlways<SignEvent> { SignManager.signProp(it) }
        eventChannel.subscribeAlways<FishStartEvent> { GamesManager.fishStart(it) }
        eventChannel.subscribeAlways<FishRollEvent> { GamesManager.fishRoll(it) }

        Log.info("事件已监听!")

        EconomyPluginConfig.firstStart = false
        Log.info("HuYanEconomy已加载！当前版本 ${description.version} !")
    }

    override fun onDisable() {
        PLUGIN_STATUS = false

        GamesManager.shutdown()
        LotteryManager.close()
        YiYanManager.shutdown()
        CronUtil.stop()
        Log.info("插件已卸载!")
    }
}

