package cn.chahuyun.economy.utils

import cn.hutool.core.date.BetweenFormatter
import cn.hutool.core.date.DateUtil
import java.text.SimpleDateFormat
import java.util.*

object TimeConvertUtil {
    /**
     * 将秒数转化为可读时间格式(HH小时mm分钟ss秒)
     */
    @JvmStatic
    fun secondConvert(time: Long): String {
        return DateUtil.formatBetween(time * 1000, BetweenFormatter.Level.SECOND)
    }

    @JvmStatic
    fun timeConvert(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return sdf.format(date)
    }
}
