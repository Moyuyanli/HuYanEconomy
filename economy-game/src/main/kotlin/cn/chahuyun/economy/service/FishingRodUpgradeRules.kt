package cn.chahuyun.economy.service

import cn.chahuyun.economy.service.FishingRodUpgradeRules.fishingLevel


/**
 * 鱼竿升级费用规则。
 *
 * [fishingLevel] 由当前鱼竿等级推导：`rodLevel == 0 -> 1 else rodLevel / 10 + 2`。
 * 返回 null 表示已达到最高等级。
 */
object FishingRodUpgradeRules {

    /** 鱼竿最高等级。 */
    const val MAX_ROD_LEVEL: Int = 99

    fun fishingLevel(rodLevel: Int): Int = if (rodLevel == 0) 1 else rodLevel / 10 + 2

    fun upgradeCost(rodLevel: Int, fishingLevel: Int): Int? = when {
        rodLevel == 0 -> 1
        rodLevel in 1..69 -> 40 * rodLevel * fishingLevel
        rodLevel in 70..79 -> 80 * rodLevel * fishingLevel
        rodLevel in 80..89 -> 100 * rodLevel * fishingLevel
        rodLevel in 90..98 -> 150 * rodLevel * fishingLevel
        rodLevel == MAX_ROD_LEVEL -> null
        else -> null
    }
}
