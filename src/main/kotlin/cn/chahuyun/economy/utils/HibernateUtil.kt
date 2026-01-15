package cn.chahuyun.economy.utils

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.config.EconomyConfig
import cn.chahuyun.hibernateplus.Configuration
import cn.chahuyun.hibernateplus.DriveType
import cn.chahuyun.hibernateplus.HibernatePlusService

/**
 * Hibernate 初始化工具
 */
object HibernateUtil {

    @JvmStatic
    fun init(economy: HuYanEconomy) {
        val config: EconomyConfig = HuYanEconomy.config

        val configuration: Configuration = HibernatePlusService.createConfiguration(economy.javaClass)
        configuration.packageName = "cn.chahuyun.economy.entity"

        val dataType: DriveType = config.dataType
        configuration.driveType = dataType
        val dataFolderPath = economy.dataFolderPath
        when (dataType) {
            DriveType.MYSQL -> {
                configuration.address = config.mysqlUrl
                configuration.user = config.mysqlUser
                configuration.password = config.mysqlPassword
            }

            DriveType.H2 -> {
                configuration.address = dataFolderPath.resolve("HuYanEconomy.h2").toString()
            }

            DriveType.SQLITE -> {
                configuration.address = dataFolderPath.resolve("HuYanEconomy").toString()
            }
        }

        HibernatePlusService.loadingService(configuration)
    }
}
