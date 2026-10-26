package cn.chahuyun.economy.games.farm

import kotlin.math.ceil

object FarmShieldPolicy {
    const val REQUIRED_LEVEL = 17
    const val DURATION_MILLIS = 12 * 60 * 60 * 1_000L

    fun activationCost(upgradeCost: Long): Long =
        ceil(upgradeCost * 0.05).toLong()
}
