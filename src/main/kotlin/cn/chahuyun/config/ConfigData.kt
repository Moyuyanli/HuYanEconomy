package cn.chahuyun.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

/**
 * 插件配置文件类
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:49
 */
object ConfigData : AutoSavePluginConfig("config") {

    @ValueDescription("插件单一管理botQQ")
    val bot: Long by value()

}