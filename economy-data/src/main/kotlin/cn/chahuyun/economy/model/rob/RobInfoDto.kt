package cn.chahuyun.economy.model.rob

import kotlinx.serialization.Serializable

/**
 * 抢劫信息DTO
 */
@Serializable
data class RobInfoDto(
    /** 用户ID（主键） */
    val userId: Long = 0,
    /** 最后一次抢劫时间 */
    val nowTime: Long = 0,
    /** 被抢次数 */
    val beRobNumber: Int = 0,
    /** 抢劫成功次数 */
    val robSuccess: Int = 0,
    /** 命中成功次数 */
    val hitSuccess: Int = 0
)
