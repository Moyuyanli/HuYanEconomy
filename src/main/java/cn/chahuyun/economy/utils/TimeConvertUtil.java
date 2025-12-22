package cn.chahuyun.economy.utils;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeConvertUtil {

    /**
     * 将秒数转化为可读时间格式(HH小时mm分钟ss秒)
     *
     * @param time 秒数时长
     * @return 格式化后的时间格式(HH小时mm分钟ss秒)
     */
    public static String secondConvert(long time) {
        return DateUtil.formatBetween(time * 1000, BetweenFormatter.Level.SECOND);
    }

    public static String timeConvert(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 格式化 Date 对象为指定的时间格式
        return sdf.format(date);
    }
}
