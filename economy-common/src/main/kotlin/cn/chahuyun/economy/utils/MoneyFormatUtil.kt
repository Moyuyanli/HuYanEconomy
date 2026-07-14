package cn.chahuyun.economy.utils

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 通用资金格式转换工具。
 *
 * - 0 ~ 999,999：显示原数，保留一位小数。
 * - 1,000,000 起按千进制缩写：M、G、T、P。
 * - 缩写最多保留一位小数，并移除无意义的 .0。
 */
object MoneyFormatUtil {

    private const val CARRY = 1000.0
    private val suffixes = charArrayOf('M', 'G', 'T', 'P')
    private val mode = RoundingMode.HALF_UP

    /**
     * 将资金数值转换为展示文本。
     *
     * 示例：`52300000.0 -> 52.3M`。
     */
    @JvmStatic
    fun format(amount: Double): String {
        if (!amount.isFinite()) return amount.toString()

        val sign = if (amount < 0) "-" else ""
        val abs = kotlin.math.abs(amount)
        if (abs < 1_000_000.0) {
            return sign + BigDecimal.valueOf(abs).setScale(1, mode).toPlainString()
        }

        var unit = 1_000_000.0
        var index = 0
        var scaled = abs / unit

        while (scaled >= CARRY && index < suffixes.lastIndex) {
            unit *= CARRY
            index++
            scaled = abs / unit
        }

        var rounded = BigDecimal.valueOf(scaled).setScale(1, mode)
        while (rounded >= BigDecimal.valueOf(CARRY) && index < suffixes.lastIndex) {
            unit *= CARRY
            index++
            scaled = abs / unit
            rounded = BigDecimal.valueOf(scaled).setScale(1, mode)
        }

        val value = rounded.stripTrailingZeros().toPlainString()
        return "$sign$value${suffixes[index]}"
    }

    /**
     * 将资金文本反向解析为数值。
     *
     * 示例：`52.3M -> 52300000.0`。
     */
    @JvmStatic
    fun parse(text: String): Double? {
        val normalized = text.trim()
        if (normalized.isBlank()) return null
        val match = Regex("""^([+-]?\d+(?:\.\d+)?)([kKmMgGtTpPwWeE万亿]?)$""").matchEntire(normalized) ?: return null
        val number = match.groupValues[1].toDoubleOrNull() ?: return null
        val multiplier = when (match.groupValues[2]) {
            "k", "K" -> 1_000.0
            "w", "W", "万" -> 10_000.0
            "m", "M" -> 1_000_000.0
            "g", "G" -> 1_000_000_000.0
            "e", "E", "亿" -> 100_000_000.0
            "t", "T" -> 1_000_000_000_000.0
            "p", "P" -> 1_000_000_000_000_000.0
            else -> 1.0
        }
        return number * multiplier
    }

    /**
     * 将 Double 类型的数值转换为资金展示文本。
     */
    fun Double.toMoneyFormat(): String = format(this)
}
