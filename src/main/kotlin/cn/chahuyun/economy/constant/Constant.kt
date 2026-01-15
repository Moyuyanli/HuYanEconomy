package cn.chahuyun.economy.constant

import cn.chahuyun.economy.model.currency.GoldEconomyCurrency
import xyz.cssxsh.mirai.economy.service.EconomyCurrency

/**
 * 固定常量
 */
object Constant {
    /**
     * 壶言日志
     */
    const val TOPIC: String = "HuYanEconomy"

    /**
     * 货币 [金币]
     */
    @JvmField
    val CURRENCY_GOLD: EconomyCurrency = GoldEconomyCurrency()
}
