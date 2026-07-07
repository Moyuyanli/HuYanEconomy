package cn.chahuyun.economy.service

import cn.hutool.core.util.RandomUtil

object BankInterestService {

    fun randomInterest(): Int {
        val roll = RandomUtil.randomInt(1, 101)
        val highInterestThreshold = 99
        val mediumInterestThreshold = 35

        return when {
            roll >= highInterestThreshold -> RandomUtil.randomInt(10, 21)
            roll >= mediumInterestThreshold -> RandomUtil.randomInt(1, 10)
            else -> RandomUtil.randomInt(-10, 1)
        }
    }
}
