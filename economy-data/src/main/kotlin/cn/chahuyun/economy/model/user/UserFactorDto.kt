package cn.chahuyun.economy.model.user

import kotlinx.serialization.Serializable

/**
 * 用户战斗因子DTO
 *
 * 记录用户的战斗属性（抢劫/战斗系统使用）
 */
@Serializable
data class UserFactorDto(
    /** 记录ID */
    val id: Long = 0,
    /** 暴躁因子 */
    val irritable: Double = 0.3,
    /** 力量因子 */
    val force: Double = 0.1,
    /** 闪避因子 */
    val dodge: Double = 0.1,
    /** 抵抗因子 */
    val resistance: Double = 0.3,
    /** 增益效果（JSON数组） */
    val buff: String = "[]"
)
