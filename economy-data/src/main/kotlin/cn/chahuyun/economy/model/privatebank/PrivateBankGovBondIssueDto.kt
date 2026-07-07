package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 国债发行DTO
 */
@Serializable
data class PrivateBankGovBondIssueDto(
    /** 记录ID */
    var id: Int = 0,
    /** 周标识（唯一） */
    var weekKey: String = "",
    /** 收益倍率 */
    var rateMultiplier: Double = 2.0,
    /** 锁定天数 */
    var lockDays: Int = 3,
    /** 总额度限制 */
    var totalLimit: Double = 0.0,
    /** 剩余额度 */
    var remaining: Double = 0.0,
    /** 创建时间 */
    var createdAt: Long = 0
)
