package cn.chahuyun.economy.utils

import cn.hutool.core.date.BetweenFormatter
import cn.hutool.core.date.DateTime
import cn.hutool.core.date.DateUnit
import java.text.SimpleDateFormat
import java.util.*
import cn.hutool.core.date.DateUtil as HutoolDateUtil

object DateUtil {

    /**
     * 日期格式化扩展函数
     * 将Date对象按照指定的模式转换为字符串格式
     *
     * @param pattern 日期格式化模式，默认为"yyyy-MM-dd HH:mm:ss"
     * @receiver Date 要格式化的日期对象
     * @return 格式化后的日期字符串
     */
    @JvmOverloads
    fun format(date: Date, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        val sdf = SimpleDateFormat(pattern)
        return sdf.format(date)
    }

    @JvmStatic
    fun formatDateTime(date: Date): String = HutoolDateUtil.formatDateTime(date)

    @JvmStatic
    fun offsetDay(date: Date, offset: Int): DateTime = HutoolDateUtil.offsetDay(date, offset)

    @JvmStatic
    fun offsetHour(date: Date, offset: Int): DateTime = HutoolDateUtil.offsetHour(date, offset)

    @JvmStatic
    fun beginOfDay(date: Date): DateTime = HutoolDateUtil.beginOfDay(date)

    @JvmStatic
    fun dayOfMonth(date: Date): Int = HutoolDateUtil.dayOfMonth(date)

    @JvmStatic
    @JvmOverloads
    fun between(beginDate: Date, endDate: Date, unit: DateUnit, isAbs: Boolean = true): Long {
        return HutoolDateUtil.between(beginDate, endDate, unit, isAbs)
    }

    @JvmStatic
    fun parse(dateStr: String): DateTime = HutoolDateUtil.parse(dateStr)

    @JvmStatic
    fun formatBetween(betweenMs: Long, level: BetweenFormatter.Level): String {
        return HutoolDateUtil.formatBetween(betweenMs, level)
    }
}

@JvmOverloads
fun Date.format(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    return DateUtil.format(this, pattern)
}
