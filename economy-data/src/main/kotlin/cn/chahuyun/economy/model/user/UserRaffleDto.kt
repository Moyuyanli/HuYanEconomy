package cn.chahuyun.economy.model.user

import kotlinx.serialization.Serializable

/**
 * 用户抽奖DTO
 *
 * 记录用户的抽奖状态和保底计数
 */
@Serializable
data class UserRaffleDto(
    /** 记录ID */
    val id: Long = 0,
    /** 默认奖池 */
    val defaultPool: String = "",
    /** 抽奖次数 */
    val times: Int = 0,
    /** 是否触发保底 */
    val jackpot: Int = 0,
    /** 各奖池抽奖次数（key=奖池名, value=次数） */
    val poolTimes: Map<String, Int> = emptyMap()
)
