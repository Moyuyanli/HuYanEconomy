package cn.chahuyun.economy.utils

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 统一的数值格式化工具。
 *
 * 说明：
 * - 业务字符串拼接优先用 Kotlin 字符串模板。
 * - 小数位/百分比等“数值格式化”集中在这里，避免在业务代码中使用 `String.format` / `"%.1f".format(...)`。
 */
object FormatUtil {

    @JvmStatic
    fun fixed(value: Double, scale: Int): String {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).toPlainString()
    }

    @JvmStatic
    fun fixed(value: Number?, scale: Int, defaultValue: String = "0"): String {
        if (value == null) return defaultValue
        return fixed(value.toDouble(), scale)
    }

    @JvmStatic
    fun round(value: Double, scale: Int): Double {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).toDouble()
    }
}
