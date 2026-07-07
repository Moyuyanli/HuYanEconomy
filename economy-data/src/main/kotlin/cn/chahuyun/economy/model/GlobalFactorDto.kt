package cn.chahuyun.economy.model

import kotlinx.serialization.Serializable

/**
 * 全局因子配置DTO
 */
@Serializable
data class GlobalFactorDto(
    /** 记录ID */
    val id: Int = 0,
    /** 抢劫因子 */
    val robFactor: Double = 0.4,
    /** 抢劫空白因子 */
    val robBlankFactor: Double = 0.01
)
