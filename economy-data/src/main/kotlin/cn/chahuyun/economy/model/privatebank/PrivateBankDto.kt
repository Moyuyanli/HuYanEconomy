package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 私人银行DTO
 */
@Serializable
data class PrivateBankDto(
    /** 记录ID */
    var id: Int = 0,
    /** 银行编码（唯一） */
    var code: String = "",
    /** 银行名称 */
    var name: String = "",
    /** 银行口号 */
    var slogan: String = "",
    /** 所有者QQ */
    var ownerQq: Long = 0,
    /** 是否仅VIP可存 */
    var vipOnly: Boolean = false,
    /** VIP白名单（JSON数组） */
    var vipWhitelist: String = "",
    /** 存款利率（百分比） */
    var depositorInterest: Int = 5,
    /** 创建时间 */
    var createdAt: Long = 0,
    /** 失信解除时间 */
    var defaulterUntil: Long = 0,
    /** 提现申请次数 */
    var withdrawRequests: Int = 0,
    /** 提现失败次数 */
    var withdrawFailures: Int = 0,
    /** 评分星级 */
    var star: Int = 1,
    /** 平均评分 */
    var avgReview: Double = 0.0
)
