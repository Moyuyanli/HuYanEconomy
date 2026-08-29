package cn.chahuyun.economy.base

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 经济系统统一金额类型。
 *
 * 内部使用 BigDecimal 保存，数据库精度为 DECIMAL(19,2)。Int、Long、Double
 * 仅作为便捷输入，进入本类型后立即转换，核心计算不再使用浮点数。
 */
@JvmInline
value class EconomyBalance private constructor(val value: BigDecimal) : Comparable<EconomyBalance> {
    companion object {
        fun of(value: BigDecimal): EconomyBalance = EconomyBalance(normalize(value))
        fun of(value: Int): EconomyBalance = of(BigDecimal.valueOf(value.toLong()))
        fun of(value: Long): EconomyBalance = of(BigDecimal.valueOf(value))
        fun of(value: Double): EconomyBalance = of(BigDecimal.valueOf(value))
        private fun normalize(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)
    }

    init {
        require(value.scale() <= 2) { "金额最多保留两位小数" }
    }

    /** 按指定小数位格式化，仅用于显示，不修改内部金额。 */
    fun format(scale: Int = 2): String = value.setScale(scale, RoundingMode.HALF_UP).toPlainString()

    /** 判断金额是否为正数。 */
    fun isPositive(): Boolean = value.signum() > 0

    /** 判断金额是否为负数。 */
    fun isNegative(): Boolean = value.signum() < 0
    override fun compareTo(other: EconomyBalance): Int = value.compareTo(other.value)
    operator fun plus(other: EconomyBalance): EconomyBalance = of(value.add(other.value))
    operator fun minus(other: EconomyBalance): EconomyBalance = of(value.subtract(other.value))
    override fun toString(): String = format()
}
