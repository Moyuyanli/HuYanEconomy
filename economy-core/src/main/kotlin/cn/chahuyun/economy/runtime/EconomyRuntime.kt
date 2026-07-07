package cn.chahuyun.economy.runtime

import cn.chahuyun.economy.config.EconomyConfig
import cn.chahuyun.economy.config.FishingMsgConfig
import cn.chahuyun.economy.config.RobMsgConfig
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import java.nio.file.Path

object EconomyRuntime {
    lateinit var plugin: JvmPlugin
        private set

    @Volatile
    var bot: Bot? = null

    @Volatile
    var pluginStatus: Boolean = false

    val config: EconomyConfig
        get() = EconomyConfig

    val msgConfig: FishingMsgConfig
        get() = FishingMsgConfig

    val robConfig: RobMsgConfig
        get() = RobMsgConfig

    val dataFolderPath: Path
        get() = plugin.dataFolderPath

    fun bind(plugin: JvmPlugin) {
        this.plugin = plugin
    }
}
