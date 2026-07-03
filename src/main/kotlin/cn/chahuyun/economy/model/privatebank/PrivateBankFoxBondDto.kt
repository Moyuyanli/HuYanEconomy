package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 狐狸债券DTO
 */
@Serializable
data class PrivateBankFoxBondDto(
    /** 记录ID */
    var id: Int = 0,
    /** 债券编码（唯一） */
    var code: String = "",
    /** 面额 */
    var faceValue: Double = 0.0,
    /** 基础利率 */
    var baseRate: Double = 0.0,
    /** 期限天数 */
    var termDays: Int = 14,
    /** 竞标开始时间 */
    var bidStartAt: Long = 0,
    /** 竞标结束时间 */
    var bidEndAt: Long = 0,
    /** 状态（BIDDING/HOLDING/FINISHED/CANCELLED） */
    var status: String = "BIDDING",
    /** 中标银行编码 */
    var winnerBankCode: String = "",
    /** 中标利率 */
    var winnerBidRate: Double = 0.0,
    /** 中标溢价 */
    var winnerPremium: Double = 0.0,
    /** 创建时间 */
    var createdAt: Long = 0
)
