package cn.chahuyun.economy.model.raffle

import kotlinx.serialization.Serializable

/**
 * 抽奖记录DTO
 */
@Serializable
data class RaffleRecordDto(
    /** 记录ID */
    val id: Long = 0,
    /** 批次ID */
    val batchId: Long = 0,
    /** 奖品ID */
    val prizeId: String = "",
    /** 奖品名称 */
    val prizeName: String = "",
    /** 奖品等级 */
    val level: Int = 0
)
