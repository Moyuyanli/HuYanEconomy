package cn.chahuyun.economy.service

import cn.chahuyun.economy.constant.FarmConstants
import cn.chahuyun.economy.entity.farm.FarmCrop
import cn.chahuyun.economy.entity.farm.FarmPlot
import kotlin.math.abs
import kotlin.math.ceil

object FarmStealRules {
    const val MIN_STEALABLE_CROP_LEVEL = 12

    fun successRate(thiefLevel: Int, targetLevel: Int): Int {
        val levelDelta = thiefLevel - targetLevel
        return when {
            levelDelta == 0 -> 20
            levelDelta > 0 -> (20 - 3 * levelDelta - 2 * levelDelta * levelDelta).coerceAtLeast(0)
            else -> (9 - abs(levelDelta)).coerceAtLeast(2)
        }
    }

    fun isStealable(plot: FarmPlot, crop: FarmCrop, now: Long): Boolean =
        plot.status == FarmConstants.PLOT_PLANTED &&
            plot.currentSeason > 0 &&
            crop.level >= MIN_STEALABLE_CROP_LEVEL &&
            crop.yieldPerSeason > 1 &&
            plot.nextMatureAt <= now &&
            plot.stolenSeason != plot.currentSeason

    fun stealAmount(
        yieldPerSeason: Int,
        thiefLevel: Int,
        targetLevel: Int,
        newcomerAmount: (Int) -> Int,
    ): Int {
        require(yieldPerSeason > 1) { "yieldPerSeason must be greater than one" }
        val levelDelta = thiefLevel - targetLevel
        val maxSteal = yieldPerSeason - 1
        val rawAmount = when {
            levelDelta < 0 -> newcomerAmount(minOf(3, maxSteal))
            levelDelta == 0 -> percentageAmount(yieldPerSeason, 20)
            targetLevel < 20 -> percentageAmount(yieldPerSeason, 30)
            else -> percentageAmount(yieldPerSeason, (20 - 2 * levelDelta).coerceAtLeast(10))
        }
        return rawAmount.coerceIn(1, maxSteal)
    }

    fun ownerHarvestAmount(plot: FarmPlot, crop: FarmCrop): Int {
        val stolen = if (plot.stolenSeason == plot.currentSeason) plot.stolenAmount else 0
        return (crop.yieldPerSeason - stolen.coerceIn(0, crop.yieldPerSeason - 1)).coerceAtLeast(1)
    }

    private fun percentageAmount(yieldPerSeason: Int, percent: Int): Int =
        ceil(yieldPerSeason * percent / 100.0).toInt()
}
