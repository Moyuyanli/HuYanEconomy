package cn.chahuyun.economy.model.user

import kotlinx.serialization.Serializable

/**
 * 用户属性DTO
 */
@Serializable
data class UserPropertyDto(
    /** 记录ID */
    val id: Long = 0
)
