package cn.chahuyun.economy.service

object FishingRodUpgradeRules {

    fun upgradeCost(rodLevel: Int, fishingLevel: Int): Int? {
        return when {
            rodLevel == 0 -> 1
            rodLevel in 1..69 -> 40 * rodLevel * fishingLevel
            rodLevel in 70..79 -> 80 * rodLevel * fishingLevel
            rodLevel in 80..89 -> 100 * rodLevel * fishingLevel
            rodLevel in 90..98 -> 150 * rodLevel * fishingLevel
            rodLevel == 99 -> 150000
            else -> null
        }
    }
}
