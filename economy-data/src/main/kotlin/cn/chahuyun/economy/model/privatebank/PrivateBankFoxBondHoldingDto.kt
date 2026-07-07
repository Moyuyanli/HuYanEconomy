package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 狐狸债券持仓DTO
 */
@Serializable
data class PrivateBankFoxBondHoldingDto(
    /** 记录ID */
    var id: Int = 0,
    /** 债券编码 */
    var bondCode: String = "",
    /** 银行编码 */
    var bankCode: String = "",
    /** 本金 */
    var principal: Double = 0.0,
    /** 利率 */
    var rate: Double = 0.0,
    /** 开始时间 */
    var startedAt: Long = 0,
    /** 到期时间 */
    var dueAt: Long = 0,
    /** 赎回时间 */
    var redeemedAt: Long = 0
)
