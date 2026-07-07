package cn.chahuyun.economy.utils

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 金额缩写格式化工具（供 Kotlin/Java 共用）。
 *
 * 格式化规则：
 * 1. 数值 < 9,999,999：显示原数，最多保留两位小数，去掉无意义的尾随零（如 100.0 -> 100）。
 * 2. 数值 >= 9,999,999：进入千进制缩写模式。
 * 3. 缩写单位：M (百万), G (十亿/Billion), T (万亿), P (千万亿)。
 * 4. 进位处理：采用标准千进制（1000进位），并处理了四舍五入导致的临界点显示问题。
 */
object MoneyFormatUtil {

    /** 触发缩写显示的阈值 */
    private const val THRESHOLD = 9_999_999.0

    /** 缩写进位基数（千进制） */
    private const val CARRY = 1000.0

    /** 缩写单位后缀 */
    private val suffixes = charArrayOf('M', 'G', 'T', 'P')

    /** 进位方式 */
    private val mode = RoundingMode.HALF_UP

    /**
     * 格式化金额
     * @param amount 原始金额
     * @return 格式化后的字符串
     */
    @JvmStatic
    fun format(amount: Double): String {
        // 处理非有限数值（如 NaN 或 Infinity）
        if (!amount.isFinite()) return amount.toString()

        val sign = if (amount < 0) "-" else ""
        val abs = kotlin.math.abs(amount)

        // 情况 A: 未达阈值，直接显示原数
        if (abs < THRESHOLD) {
            return sign + formatPlain(abs)
        }

        // 情况 B: 超过阈值，计算缩写
        var unit = 1_000_000.0 // 起始单位：M
        var idx = 0
        var scaled = abs / unit

        // 进位判断
        while (scaled >= CARRY - 0.005 && idx < suffixes.lastIndex) {
            unit *= CARRY
            idx++
            scaled = abs / unit
        }

        // 最终保留两位小数并四舍五入
        val rounded = BigDecimal.valueOf(scaled)
            .setScale(2, mode)
            .toPlainString()

        return "$sign$rounded${suffixes[idx]}"
    }

    /**
     * 格式化原始数值，移除无意义的 .0
     */
    private fun formatPlain(abs: Double): String {
        val asLong = abs.toLong()
        // 如果是整数，直接返回整数字符串
        if (abs == asLong.toDouble()) return asLong.toString()

        // 如果是浮点数，保留两位小数并去掉末尾多余的 0
        return BigDecimal.valueOf(abs)
            .setScale(2, mode)
            .stripTrailingZeros()
            .toPlainString()
    }


    /**
     * 将Double类型的数值转换为货币格式的字符串
     *
     * 此函数为Double类型的扩展函数，直接对调用者进行格式化
     *
     * @return 格式化后的货币字符串
     */
    fun Double.toMoneyFormat() = format(this)

}