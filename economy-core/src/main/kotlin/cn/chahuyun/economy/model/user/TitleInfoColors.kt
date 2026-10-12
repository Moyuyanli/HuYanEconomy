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


/*
 * TODO(action-chain): 钓鱼动作与规则链骨架。
 *
 * 目标框架见 docs/KSP 驱动的 Action 规则链框架 · 设计文档.md：
 *   @Action("game.fish")                                        -> 动作本体
 *   @Before("game.fish", order = ..., compensate = ...)         -> 前置规则（冷却、称号、鱼饵消耗…）
 *   @After("game.fish",  order = ..., compensate = ...)         -> 后置规则（结算、称号授予…）
 *
 * 规则链框架尚未实现，这里仅保留迁移指引；原占位代码不是合法 Kotlin 语法，故以注释形式保留。
 *
 * 当前钓鱼玩法已在 `FishAction`(economy-main) + `FishingUsecase`(economy-game) 上按新实体重建，
 * 待框架落地后可把 usecase 内的横切逻辑拆分为 @Before/@After 规则。
 */