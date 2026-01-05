package cn.chahuyun.economy.model.currency

import xyz.cssxsh.mirai.economy.service.EconomyCurrency

/**
 * 金币货币模型
 */
class GoldEconomyCurrency : EconomyCurrency {
    override val id: String = "hy-gold"
    override val name: String = "金币"
    override val description: String = "壶言经济金币"
}

