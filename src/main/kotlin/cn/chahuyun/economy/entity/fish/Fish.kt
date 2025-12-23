package cn.chahuyun.economy.entity.fish

import cn.hutool.core.util.RandomUtil
import jakarta.persistence.*
import java.io.Serializable
import kotlin.math.roundToInt

/**
 * 鱼
 *
 * @author Moyuyanli
 * @date 2022/12/9 9:50
 */
@Entity(name = "Fish")
@Table
class Fish(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    /**
     * 等级
     */
    var level: Int = 0,

    /**
     * 名称
     */
    var name: String? = null,

    /**
     * 描述
     */
    var description: String? = null,

    /**
     * 单价
     */
    var price: Int = 0,

    /**
     * 最小尺寸
     */
    var dimensionsMin: Int = 0,

    /**
     * 最大尺寸
     */
    var dimensionsMax: Int = 0,

    var dimensions1: Int = 0,
    var dimensions2: Int = 0,
    var dimensions3: Int = 0,
    var dimensions4: Int = 0,

    /**
     * 难度
     */
    var difficulty: Int = 0,

    /**
     * 特殊标记
     */
    var special: Boolean = false
) : Serializable {

    /**
     * 获取鱼的尺寸
     *
     * @param winning 当难度随机到200时，尺寸+20%
     * @return 鱼的尺寸
     */
    fun getDimensions(winning: Boolean): Int {
        val i = RandomUtil.randomInt(0, 101)
        val randomInt = when {
            i >= 90 -> RandomUtil.randomInt(dimensions3, if (dimensions4 == dimensions3) dimensions4 + 1 else dimensions4)
            i >= 70 -> RandomUtil.randomInt(dimensions2, if (dimensions3 == dimensions2) dimensions3 + 1 else dimensions3)
            else -> RandomUtil.randomInt(dimensions1, if (dimensions2 == dimensions1) dimensions2 + 1 else dimensions2)
        }
        return if (winning) {
            (randomInt + (randomInt * 0.2)).toInt()
        } else {
            randomInt
        }
    }

    /**
     * 惊喜尺寸
     *
     * @param surprise 是否惊喜
     * @param evolution 进化因子
     * @return 鱼的尺寸
     */
    fun getSurprise(surprise: Boolean, evolution: Float): Int {
        val dimensions = getDimensions(false)
        return if (surprise) {
            (dimensions * (1 + evolution)).roundToInt()
        } else {
            dimensions
        }
    }
}
