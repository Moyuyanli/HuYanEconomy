package cn.chahuyun.economy.constant

import cn.chahuyun.economy.model.currency.GoldEconomyCurrency
import xyz.cssxsh.mirai.economy.service.EconomyCurrency

/**
 * 固定常量
 */
object Constant {
    /**
     * 壶言经济日志主题名。
     */
    const val TOPIC: String = "HuYanEconomy"

    /**
     * 插件默认货币：金币。
     */
    @JvmField
    val CURRENCY_GOLD: EconomyCurrency = GoldEconomyCurrency()
}
