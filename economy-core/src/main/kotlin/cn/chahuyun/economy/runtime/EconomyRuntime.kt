package cn.chahuyun.economy.runtime

import cn.chahuyun.economy.config.EconomyConfig
import cn.chahuyun.economy.config.FishingMsgConfig
import cn.chahuyun.economy.config.RobMsgConfig
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import java.nio.file.Path

/**
 * 跨模块运行时门面。
 *
 * core/data/game/image 等模块不应直接依赖 main 模块的 HuYanEconomy 单例。
 * 插件启动时由 main 绑定实际 JvmPlugin，本对象再向下游模块暴露配置、bot 和 data 目录。
 */
object EconomyRuntime {
    /** 当前绑定的 mirai 插件实例，必须在 onLoad/onEnable 早期完成绑定。 */
    lateinit var plugin: JvmPlugin
        private set

    /** 当前绑定的 bot；插件仅支持单 bot，在线事件会更新该值。 */
    @Volatile
    var bot: Bot? = null

    /** 插件是否处于运行状态，供定时任务或后台任务快速判断。 */
    @Volatile
    var pluginStatus: Boolean = false

    /** 主配置对象，始终读取 AutoSavePluginConfig 单例的当前值。 */
    val config: EconomyConfig
        get() = EconomyConfig

    /** 钓鱼文案配置。 */
    val msgConfig: FishingMsgConfig
        get() = FishingMsgConfig

    /** 抢劫文案配置。 */
    val robConfig: RobMsgConfig
        get() = RobMsgConfig

    /** 插件 data 目录路径，用于资源、数据库文件等运行期文件。 */
    val dataFolderPath: Path
        get() = plugin.dataFolderPath

    /** 绑定插件实例；重复绑定会覆盖旧值，便于 mirai 生命周期重载后恢复引用。 */
    fun bind(plugin: JvmPlugin) {
        this.plugin = plugin
    }
}
