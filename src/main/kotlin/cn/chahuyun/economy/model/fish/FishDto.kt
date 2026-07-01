package cn.chahuyun.economy.model.fish

import cn.hutool.core.util.RandomUtil
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * 鱼种DTO
 */
@Serializable
data class FishDto(
    /** 记录ID */
    var id: Int = 0,
    /** 鱼的等级 */
    var level: Int = 0,
    /** 鱼的名称 */
    var name: String = "",
    /** 鱼的描述 */
    var description: String = "",
    /** 鱼的价格 */
    var price: Int = 0,
    /** 最小尺寸 */
    var dimensionsMin: Int = 0,
    /** 最大尺寸 */
    var dimensionsMax: Int = 0,
    /** 尺寸阈值1 */
    var dimensions1: Int = 0,
    /** 尺寸阈值2 */
    var dimensions2: Int = 0,
    /** 尺寸阈值3 */
    var dimensions3: Int = 0,
    /** 尺寸阈值4 */
    var dimensions4: Int = 0,
    /** 难度 */
    var difficulty: Int = 0,
    /** 是否为特殊鱼 */
    var special: Boolean = false
) {
    fun getDimensions(winning: Boolean): Int {
        val i = RandomUtil.randomInt(0, 101)
        val randomInt = when {
            i >= 90 -> RandomUtil.randomInt(dimensions3, if (dimensions4 == dimensions3) dimensions4 + 1 else dimensions4)
            i >= 70 -> RandomUtil.randomInt(dimensions2, if (dimensions3 == dimensions2) dimensions3 + 1 else dimensions3)
            else -> RandomUtil.randomInt(dimensions1, if (dimensions2 == dimensions1) dimensions2 + 1 else dimensions2)
        }
        return if (winning) (randomInt + (randomInt * 0.2)).toInt() else randomInt
    }

    fun getSurprise(surprise: Boolean, evolution: Float): Int {
        val dimensions = getDimensions(false)
        return if (surprise) (dimensions * (1 + evolution)).roundToInt() else dimensions
    }
}
