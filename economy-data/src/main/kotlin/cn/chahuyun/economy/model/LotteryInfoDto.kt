package cn.chahuyun.economy.model

import kotlinx.serialization.Serializable

/**
 * 彩票信息DTO
 */
@Serializable
data class LotteryInfoDto(
    /** 记录ID */
    val id: Int = 0,
    /** QQ号 */
    val qq: Long = 0,
    /** 群号 */
    val group: Long = 0,
    /** 投注金额 */
    val money: Double = 0.0,
    /** 彩票类型 */
    val type: Int = 0,
    /** 选号 */
    val number: String = "",
    /** 当期号码 */
    val current: String = "",
    /** 奖金 */
    val bonus: Double = 0.0
)
