package cn.chahuyun.economy.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeConvertUtil {
    public static String timeConvert(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 将毫秒级时间戳转换为 Date 对象
        Date date = new Date(time);

        // 格式化 Date 对象为指定的时间格式
        return sdf.format(date);
    }
}
