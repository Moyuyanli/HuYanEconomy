package cn.chahuyun.economy.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtil {

    /**
     * 日期格式化扩展函数
     * 将Date对象按照指定的模式转换为字符串格式
     *
     * @param pattern 日期格式化模式，默认为"yyyy-MM-dd HH:mm:ss"
     * @receiver Date 要格式化的日期对象
     * @return 格式化后的日期字符串
     */
    @JvmStatic
    @JvmOverloads
    fun Date.format(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        val sdf = SimpleDateFormat(pattern)
        return sdf.format(this)
    }

}