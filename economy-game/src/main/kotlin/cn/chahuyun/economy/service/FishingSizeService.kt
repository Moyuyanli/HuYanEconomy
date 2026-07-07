package cn.chahuyun.economy.service

import cn.chahuyun.economy.model.fish.FishDto
import cn.hutool.core.util.RandomUtil
import kotlin.math.roundToInt

object FishingSizeService {

    fun dimensions(fish: FishDto, winning: Boolean): Int {
        val roll = RandomUtil.randomInt(0, 101)
        val baseDimensions = when {
            roll >= 90 -> RandomUtil.randomInt(fish.dimensions3, exclusiveUpper(fish.dimensions3, fish.dimensions4))
            roll >= 70 -> RandomUtil.randomInt(fish.dimensions2, exclusiveUpper(fish.dimensions2, fish.dimensions3))
            else -> RandomUtil.randomInt(fish.dimensions1, exclusiveUpper(fish.dimensions1, fish.dimensions2))
        }
        return if (winning) (baseDimensions + (baseDimensions * 0.2)).toInt() else baseDimensions
    }

    fun surpriseDimensions(fish: FishDto, surprise: Boolean, evolution: Float): Int {
        val dimensions = dimensions(fish, winning = false)
        return if (surprise) (dimensions * (1 + evolution)).roundToInt() else dimensions
    }

    private fun exclusiveUpper(lower: Int, upper: Int): Int =
        if (upper == lower) upper + 1 else upper
}
