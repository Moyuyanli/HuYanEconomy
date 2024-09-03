package cn.chahuyun.economy.config

import cn.chahuyun.hibernateplus.DriveType
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

/**
 * 插件配置文件类
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:49
 */
object EconomyConfig : AutoSavePluginConfig("Config") {

    @ValueDescription("主人\n")
    var owner: Long by value(123456L)

    @ValueDescription("指令触发前缀,空白则没有前缀(暂时不可用)")
    var prefix: String by value(" ")

    @ValueDescription("每日签到刷新时间(0-23)\n")
    var reSignTime : Int by value(4)

    @ValueDescription("数据库类型(H2,MYSQL,SQLITE)")
    var dataType: DriveType by value(DriveType.H2)

    @ValueDescription("mysql 连接地址")
    var mysqlUrl: String by value("localhost:3306/test")

    @ValueDescription("mysql 用户名")
    var mysqlUser: String by value("root")

    @ValueDescription("mysql 密码")
    var mysqlPassword: String by value("123456")

    @ValueDescription("插件单一管理botQQ\n")
    val bot: Long by value()

}