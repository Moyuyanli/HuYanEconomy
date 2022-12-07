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
object EconomyConfig : AutoSavePluginConfig("config") {

    @ValueDescription("主人\n")
    var owner: Long by value()

    @ValueDescription("插件单一管理botQQ\n")
    val bot: Long by value()

    @ValueDescription("启用的彩票群列表\n")
    var group: List<Long> by value()


}