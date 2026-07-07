package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 私人银行存款DTO
 */
@Serializable
data class PrivateBankDepositDto(
    /** 记录ID */
    var id: Int = 0,
    /** 银行编码 */
    var bankCode: String = "",
    /** 存款人QQ */
    var userQq: Long = 0,
    /** 本金 */
    var principal: Double = 0.0,
    /** 存入时间 */
    var createdAt: Long = 0,
    /** 更新时间 */
    var updatedAt: Long = 0
)
