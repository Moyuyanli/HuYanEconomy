package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 私人银行评价DTO
 */
@Serializable
data class PrivateBankReviewDto(
    /** 记录ID */
    var id: Int = 0,
    /** 银行编码 */
    var bankCode: String = "",
    /** 评价人QQ */
    var userQq: Long = 0,
    /** 评分（1-5） */
    var rating: Int = 5,
    /** 评价内容 */
    var content: String = "",
    /** 创建时间 */
    var createdAt: Long = 0
)
