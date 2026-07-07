package cn.chahuyun.economy.model.user

import kotlinx.serialization.Serializable

/**
 * 称号信息DTO
 */
@Serializable
data class TitleInfoDto(
    /** 记录ID */
    val id: Int = 0,
    /** 所属用户ID */
    val userId: Long = 0,
    /** 称号编码 */
    val code: String = "",
    /** 称号名称 */
    val name: String = "",
    /** 是否激活 */
    val status: Boolean = false,
    /** 称号文本 */
    val title: String = "",
    /** 是否影响昵称显示 */
    val impactName: Boolean = false,
    /** 是否渐变色 */
    val gradient: Boolean = false,
    /** 起始颜色 */
    val sColor: String = "",
    /** 结束颜色 */
    val eColor: String = "",
    /** 到期时间 */
    val dueTime: Long = 0
)
