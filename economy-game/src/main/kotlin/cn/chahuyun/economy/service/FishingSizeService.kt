package cn.chahuyun.economy.service

import cn.chahuyun.economy.entity.fishing.FishEntity
import cn.hutool.core.util.RandomUtil
import kotlin.math.roundToInt

/**
 * 鱼类尺寸计算。
 *
 * - 普通尺寸：按 `dimensions1~4` 的分位区间随机；
 * - 胜利加成：难度博弈成功时尺寸 +20%；
 * - 惊喜尺寸：命中惊喜时按进化因子放大后四舍五入。
 */
object FishingSizeService {

    fun dimensions(fish: FishEntity, winning: Boolean): Int {
        val roll = RandomUtil.randomInt(0, 101)
        val baseDimensions = when {
            roll >= 90 -> RandomUtil.randomInt(fish.dimensions3, exclusiveUpper(fish.dimensions3, fish.dimensions4))
            roll >= 70 -> RandomUtil.randomInt(fish.dimensions2, exclusiveUpper(fish.dimensions2, fish.dimensions3))
            else -> RandomUtil.randomInt(fish.dimensions1, exclusiveUpper(fish.dimensions1, fish.dimensions2))
        }
        return if (winning) (baseDimensions + (baseDimensions * 0.2)).toInt() else baseDimensions
    }

    fun surpriseDimensions(fish: FishEntity, surprise: Boolean, evolution: Float): Int {
        val dimensions = dimensions(fish, winning = false)
        return if (surprise) (dimensions * (1 + evolution)).roundToInt() else dimensions
    }

    private fun exclusiveUpper(lower: Int, upper: Int): Int =
        if (upper == lower) upper + 1 else upper
}
