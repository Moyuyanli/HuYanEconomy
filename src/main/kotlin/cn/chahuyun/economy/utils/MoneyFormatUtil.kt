package cn.chahuyun.economy.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

/**
 * 金额缩写格式化工具（供 Kotlin/Java 共用）。
 *
 * 规则（按你的描述实现）：
 * - 小于 9,999,999：直接显示原数值（尽量避免无意义的 .0）
 * - 之后：按千进制缩写，单位为 M/G/T/P（M=百万，G=1000M，T=1000G，P=1000T）
 * - 缩写展示避免出现三位数（>=100 的缩写值会继续进位到下一档）
 * - 缩写数值保留两位小数
 */
object MoneyFormatUtil {

    private const val THRESHOLD = 9_999_999.0

    private val suffixes = charArrayOf('M', 'G', 'T', 'P')

    @JvmStatic
    fun format(amount: Double): String {
        if (!amount.isFinite()) return amount.toString()

        val sign = if (amount < 0) "-" else ""
        val abs = kotlin.math.abs(amount)

        // 9999999 以下直接显示
        if (abs < THRESHOLD) {
            return sign + formatPlain(abs)
        }

        var unit = 1_000_000.0
        var idx = 0
        var scaled = abs / unit

        // 避免三位数展示：>=100 则进位到下一档
        while (scaled >= 100.0 && idx < suffixes.lastIndex) {
            unit *= 1000.0
            idx++
            scaled = abs / unit
        }

        val rounded = BigDecimal.valueOf(scaled).setScale(2, RoundingMode.HALF_UP).toPlainString()
        return sign + String.format(Locale.ROOT, "%s%c", rounded, suffixes[idx])
    }

    /**
     * 格式化浮点数为字符串，避免显示尾随零
     *
     * @param abs 需要格式化的绝对值浮点数
     * @return 格式化后的字符串表示
     */
    private fun formatPlain(abs: Double): String {
        // 尽量避免 1.0 这种显示
        val asLong = abs.toLong()
        if (abs == asLong.toDouble()) return asLong.toString()

        // 保留两位小数并去掉尾随 0
        val bd = BigDecimal.valueOf(abs).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()
        return bd.toPlainString()
    }

}


