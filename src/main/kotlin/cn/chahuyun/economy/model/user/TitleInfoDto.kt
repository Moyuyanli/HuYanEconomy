package cn.chahuyun.economy.model.user

import kotlinx.serialization.Serializable
import java.awt.Color

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
) {
    val startColor: Color
        get() = parseColor(sColor.ifBlank { "8a8886" })

    val endColor: Color
        get() = parseColor(eColor.ifBlank { sColor.ifBlank { "8a8886" } })

    private fun parseColor(color: String): Color {
        val hex = if (color.startsWith("#")) color.substring(1) else color
        require(hex.length == 6) { "Invalid title color: $color" }
        return Color(
            hex.substring(0, 2).toInt(16),
            hex.substring(2, 4).toInt(16),
            hex.substring(4, 6).toInt(16)
        )
    }
}
