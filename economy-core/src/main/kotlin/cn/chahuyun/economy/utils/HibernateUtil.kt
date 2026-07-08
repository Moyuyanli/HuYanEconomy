package cn.chahuyun.economy.utils

import cn.chahuyun.economy.config.EconomyConfig
import cn.chahuyun.economy.runtime.EconomyRuntime
import cn.chahuyun.hibernateplus.Configuration
import cn.chahuyun.hibernateplus.DriveType
import cn.chahuyun.hibernateplus.HibernatePlusService
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin

/**
 * Hibernate 初始化工具。
 *
 * 实体扫描包固定为 `cn.chahuyun.economy.entity`，实际 SessionFactory 由 hibernate-plus 插件提供。
 * 本工具只负责把壶言经济配置翻译成 hibernate-plus 的 Configuration。
 */
object HibernateUtil {

    @JvmStatic
    fun init(economy: JvmPlugin) {
        val config: EconomyConfig = EconomyRuntime.config

        val configuration: Configuration = HibernatePlusService.createConfiguration(economy.javaClass)
        configuration.packageName = "cn.chahuyun.economy.entity"

        val dataType: DriveType = config.dataType
        configuration.driveType = dataType
        val dataFolderPath = economy.dataFolderPath
        when (dataType) {
            DriveType.MYSQL -> {
                // MySQL 使用用户配置的远程连接信息。
                configuration.address = config.mysqlUrl
                configuration.user = config.mysqlUser
                configuration.password = config.mysqlPassword
            }

            DriveType.H2 -> {
                // H2 文件放在插件 data 目录，和 mirai-console 实例绑定。
                configuration.address = dataFolderPath.resolve("HuYanEconomy.h2").toString()
            }

            DriveType.SQLITE -> {
                // SQLite 同样落在插件 data 目录，hibernate-plus 会补齐具体文件后缀。
                configuration.address = dataFolderPath.resolve("HuYanEconomy").toString()
            }

            else -> {
                // 兜底：框架新增 DriveType 时避免编译失败；运行时按需补充配置项
                configuration.address = dataFolderPath.resolve("HuYanEconomy").toString()
            }
        }

        HibernatePlusService.loadingService(configuration)
    }
}
