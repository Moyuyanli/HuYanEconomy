package cn.chahuyun.economy.model.user

import java.awt.Color

val TitleInfoDto.startColor: Color
    get() = parseTitleColor(sColor.ifBlank { DEFAULT_TITLE_COLOR })

val TitleInfoDto.endColor: Color
    get() = parseTitleColor(eColor.ifBlank { sColor.ifBlank { DEFAULT_TITLE_COLOR } })

private const val DEFAULT_TITLE_COLOR = "8a8886"

private fun parseTitleColor(color: String): Color {
    val hex = if (color.startsWith("#")) color.substring(1) else color
    require(hex.length == 6) { "Invalid title color: $color" }
    return Color(
        hex.substring(0, 2).toInt(16),
        hex.substring(2, 4).toInt(16),
        hex.substring(4, 6).toInt(16)
    )
}
