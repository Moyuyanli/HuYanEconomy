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

    @ValueDescription("指令触发前缀")
    var prefix: String by value("")

    @ValueDescription("每日签到刷新时间(0-23)\n")
    var reSignTime: Int by value(4)

    @ValueDescription("数据库类型(H2,MYSQL,SQLITE)")
    var dataType: DriveType by value(DriveType.H2)

    @ValueDescription(
        """
        经济后端
        BASE           : 使用本插件自建账户 economy_account（默认，不依赖前置插件）
        MIRAI_ECONOMY  : 使用上游 mirai-economy 插件（需自行安装该前置插件）
        注意：切换后端不会自动搬迁余额，切换前请先执行 hye economy migrate
        """
    )
    var economyBackend: EconomyBackend by value(EconomyBackend.BASE)

    @ValueDescription("配置选择 MIRAI_ECONOMY 但探测不到上游插件时，是否降级为 BASE（true 降级并告警，false 启动失败）")
    var economyBackendFallback: Boolean by value(true)

    @ValueDescription("mysql 连接地址")
    var mysqlUrl: String by value("localhost:3306/test")

    @ValueDescription("mysql 用户名")
    var mysqlUser: String by value("root")

    @ValueDescription("mysql 密码")
    var mysqlPassword: String by value("123456")

    @ValueDescription("用户下一条消息等待线程数")
    val nextMessageExecutorsNumber: Int by value(10)

    @ValueDescription("无法使用禁言卡的群")
    val unableToUseMuteGroup: MutableList<Long> by value(mutableListOf())

}