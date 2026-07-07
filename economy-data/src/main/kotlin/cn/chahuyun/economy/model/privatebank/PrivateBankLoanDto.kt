package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 私人银行借款记录DTO
 */
@Serializable
data class PrivateBankLoanDto(
    /** 记录ID */
    var id: Int = 0,
    /** 贷款产品ID */
    var offerId: Int = 0,
    /** 银行编码 */
    var bankCode: String = "",
    /** 贷方QQ */
    var lenderQq: Long = 0,
    /** 借方QQ */
    var borrowerQq: Long = 0,
    /** 本金 */
    var principal: Double = 0.0,
    /** 应还总额 */
    var dueTotal: Double = 0.0,
    /** 已还金额 */
    var repaidAmount: Double = 0.0,
    /** 利率（内部单位 0.1%） */
    var interest: Int = 10,
    /** 借款天数 */
    var termDays: Int = 7,
    /** 创建时间 */
    var createdAt: Long = 0,
    /** 到期时间 */
    var dueAt: Long = 0,
    /** 还清时间 */
    var repaidAt: Long = 0
)
