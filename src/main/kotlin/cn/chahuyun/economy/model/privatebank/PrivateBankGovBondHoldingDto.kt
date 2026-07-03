package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 国债持仓DTO
 */
@Serializable
data class PrivateBankGovBondHoldingDto(
    /** 记录ID */
    var id: Int = 0,
    /** 银行编码 */
    var bankCode: String = "",
    /** 发行ID */
    var issueId: Int = 0,
    /** 本金 */
    var principal: Double = 0.0,
    /** 收益倍率 */
    var rateMultiplier: Double = 2.0,
    /** 锁定天数 */
    var lockDays: Int = 3,
    /** 买入时间 */
    var boughtAt: Long = 0,
    /** 赎回时间 */
    var redeemedAt: Long = 0
)
