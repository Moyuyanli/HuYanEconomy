package cn.chahuyun.config

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
    var owner: List<Long> by value()

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

    @ValueDescription("启用的彩票群列表\n")
    var lotteryGroup: List<Long> by value()

    @ValueDescription("启用的钓鱼群列表\n")
    var fishGroup: List<Long> by value()


}