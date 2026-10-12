package cn.chahuyun.economy.model.user

import net.mamoe.mirai.event.events.MessageEvent
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


class FishAction {

    @Action("game.fish")
    fun fish(event: MessageEvent, userInfoDto: UserInfoDto) {

    }


}


object FishRules {

    @Before("game.fish",这里可以填一些其他相关参数,优先级,回滚策略等)
    fun titleCheck(t: UserInfoDto) {

    }


}