package cn.chahuyun.economy.service

import kotlin.math.ceil

object FarmShieldRules {
    const val REQUIRED_LEVEL = 17
    const val DURATION_MILLIS = 12 * 60 * 60 * 1_000L

    fun activationCost(upgradeCost: Long): Long =
        ceil(upgradeCost * 0.05).toLong()
}
