package cn.chahuyun.economy.common.math

import kotlin.math.round
import kotlin.random.Random

object Probability {
    @JvmStatic
    fun percentToInt(value: Double): Int {
        return round(value * 100).toInt()
    }

    @JvmStatic
    fun oneDecimal(value: Double): Double {
        return round(value * 10.0) / 10.0
    }

    @JvmStatic
    fun hit(percent: Int, random: Random = Random.Default): Boolean {
        require(percent in 0..100) { "Probability must be between 0 and 100" }
        return random.nextInt(1, 101) <= percent
    }
}
