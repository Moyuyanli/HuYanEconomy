package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 私人银行贷款产品DTO
 */
@Serializable
data class PrivateBankLoanOfferDto(
    /** 记录ID */
    var id: Int = 0,
    /** 银行编码 */
    var bankCode: String = "",
    /** 所有者QQ */
    var ownerQq: Long = 0,
    /** 资金来源 */
    var source: String = "LIQUIDITY",
    /** 总额度 */
    var total: Double = 0.0,
    /** 剩余额度 */
    var remaining: Double = 0.0,
    /** 利率（内部单位 0.1%） */
    var interest: Int = 10,
    /** 借款天数 */
    var termDays: Int = 7,
    /** 是否启用 */
    var enabled: Boolean = true,
    /** 创建时间 */
    var createdAt: Long = 0
)
