package cn.chahuyun.economy.model.raffle

import kotlinx.serialization.Serializable

/**
 * 抽奖批次DTO
 */
@Serializable
data class RaffleBatchDto(
    /** 批次ID */
    val id: Long = 0,
    /** 用户ID */
    val userId: Long = 0,
    /** 群号 */
    val groupId: Long = 0,
    /** 奖池ID */
    val poolId: String = "",
    /** 抽奖类型 */
    val raffleType: String = "",
    /** 创建时间 */
    val createTime: Long = 0,
    /** 抽奖记录数 */
    val recordCount: Int = 0,
    /** 抽奖记录 */
    val records: List<RaffleRecordDto> = emptyList()
)
