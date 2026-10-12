package cn.chahuyun.economy.base

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 经济系统统一金额类型。
 *
 * 内部使用 BigDecimal 保存，数据库精度为 DECIMAL(19,2)。Int、Long、Double
 * 仅作为便捷输入，进入本类型后立即转换，核心计算不再使用浮点数。
 *
 * 金额口径（对应 plan/v2.0.0/2.0新经济系统设计.md §7、§9）：
 * - 小数位：超过两位小数不拒绝，统一按 [RoundingMode.HALF_UP] 舍入到两位；
 * - 取值范围：绝对值不得超过 [MAX_ABS]（DECIMAL(19,2) 能表示的最大值），
 *   由 [isWithinRange] 判定，并在经济操作入口处拒绝越界金额。
 */
@JvmInline
value class EconomyBalance private constructor(val value: BigDecimal) : Comparable<EconomyBalance> {
    companion object {
        /**
         * DECIMAL(19,2) 可表示的最大绝对值：17 位整数 + 2 位小数。
         * 超出该值的金额写入数据库会溢出，因此在操作入口被拒绝为 INVALID_AMOUNT。
         */
        val MAX_ABS: BigDecimal = BigDecimal("99999999999999999.99")

        fun of(value: BigDecimal): EconomyBalance = EconomyBalance(normalize(value))
        fun of(value: Int): EconomyBalance = of(BigDecimal.valueOf(value.toLong()))
        fun of(value: Long): EconomyBalance = of(BigDecimal.valueOf(value))
        fun of(value: Double): EconomyBalance = of(BigDecimal.valueOf(value))

        /** 统一舍入规则：固定两位小数，采用 HALF_UP。 */
        private fun normalize(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)
    }

    /** 按指定小数位格式化，仅用于显示，不修改内部金额。 */
    fun format(scale: Int = 2): String = value.setScale(scale, RoundingMode.HALF_UP).toPlainString()

    /** 判断金额是否为正数。 */
    fun isPositive(): Boolean = value.signum() > 0

    /** 判断金额是否为负数。 */
    fun isNegative(): Boolean = value.signum() < 0

    /** 判断金额绝对值是否落在 DECIMAL(19,2) 可表示的范围内。 */
    fun isWithinRange(): Boolean = value.abs() <= MAX_ABS

    /**
     * 是否为一次经济操作的合法金额：必须为正数且不超过 [MAX_ABS]。
     * 这是 EconomyCore 在扣款、入账、转账前的统一校验口径。
     */
    fun isUsableAmount(): Boolean = isPositive() && isWithinRange()
    override fun compareTo(other: EconomyBalance): Int = value.compareTo(other.value)
    operator fun plus(other: EconomyBalance): EconomyBalance = of(value.add(other.value))
    operator fun minus(other: EconomyBalance): EconomyBalance = of(value.subtract(other.value))
    override fun toString(): String = format()
}
