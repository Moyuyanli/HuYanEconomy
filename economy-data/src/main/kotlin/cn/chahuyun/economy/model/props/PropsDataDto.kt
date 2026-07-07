package cn.chahuyun.economy.model.props

import kotlinx.serialization.Serializable

/**
 * 道具数据DTO
 */
@Serializable
data class PropsDataDto(
    /** 记录ID */
    val id: Long = 0,
    /** 道具种类 */
    val kind: String = "",
    /** 道具编码 */
    val code: String = "",
    /** 数量 */
    val num: Int = 1,
    /** 过期时间 */
    val expiredTime: Long = 0,
    /** 是否已使用 */
    val status: Boolean = false,
    /** 附加数据（JSON） */
    val data: String = ""
)
