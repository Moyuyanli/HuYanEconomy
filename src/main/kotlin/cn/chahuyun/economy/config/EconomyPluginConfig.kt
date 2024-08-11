package cn.chahuyun.economy.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

/**
 * 插件配置文件类
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:49
 */
object EconomyPluginConfig : AutoSavePluginConfig("PluginConfig") {

    @ValueDescription("插件是否第一次加载")
    var firstStart: Boolean by value(true)

}