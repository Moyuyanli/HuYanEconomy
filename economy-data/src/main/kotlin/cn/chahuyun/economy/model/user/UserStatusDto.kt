package cn.chahuyun.economy.model.user

import kotlinx.serialization.Serializable

/**
 * 用户状态DTO
 *
 * 记录用户当前位置（家/医院/监狱/鱼塘等）
 */
@Serializable
data class UserStatusDto(
    /** 记录ID */
    val id: Long = 0,
    /** 当前位置 */
    val place: String = "HOME",
    /** 恢复剩余时间（秒） */
    val recoveryTime: Int = 0,
    /** 状态开始时间 */
    val startTime: Long = 0
)
