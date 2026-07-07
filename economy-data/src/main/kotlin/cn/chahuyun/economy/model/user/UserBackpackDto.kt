package cn.chahuyun.economy.model.user

import kotlinx.serialization.Serializable

/**
 * 用户背包DTO
 */
@Serializable
data class UserBackpackDto(
    /** 背包记录ID */
    val id: Long = 0,
    /** 所属用户ID */
    val userId: String = "",
    /** 道具编码 */
    val propCode: String = "",
    /** 道具种类 */
    val propKind: String = "",
    /** 道具ID */
    val propId: Long = 0
)
