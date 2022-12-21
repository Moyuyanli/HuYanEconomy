package cn.chahuyun.economy.utils;

import cn.chahuyun.economy.HuYanEconomy;
import net.mamoe.mirai.utils.MiraiLogger;

/**
 * 日志<p>
 * 使用此类日志做统一处理
 *
 * @author Moyuyanli
 * @date 2022/11/14 13:01
 */
public class Log {

    private static final MiraiLogger log = HuYanEconomy.INSTANCE.getLogger();

    private static String name = "壶言经济--";

    private Log() {

    }

    /**
     * 提示级信息
     *
     * @param msg 日志信息
     * @author Moyuyanli
     * @date 2022/11/14 13:04
     */
    public static void info(String msg) {
        log.info(name + msg);
    }


    /**
     * 警告级信息
     *
     * @param msg 日志信息
     * @author Moyuyanli
     * @date 2022/11/14 13:23
     */
    public static void warning(String msg) {
        log.warning(name + msg);
    }

    /**
     * 错误级信息
     *
     * @param msg 消息
     * @author Moyuyanli
     * @date 2022/11/14 13:25
     */
    public static void error(String msg) {
        log.error(name + msg);
    }

    /**
     * 错误级信息
     *
     * @param e 异常
     * @author Moyuyanli
     * @date 2022/11/14 15:20
     */

    public static void error(Throwable e) {
        log.error(name + e.getMessage(), e);
    }

    /**
     * 错误级信息
     *
     * @param msg 消息
     * @param e   异常
     * @author Moyuyanli
     * @date 2022/11/14 15:19
     */
    public static void error(String msg, Throwable e) {
        log.error(name + msg);
        log.error(name + e.getMessage(), e);
    }

    /**
     * 调试级信息
     *
     * @param msg 消息
     * @author Moyuyanli
     * @date 2022/11/14 14:44
     */
    public static void debug(String msg) {
        log.debug(name + msg);
    }

    /**
     * 调试级信息
     *
     * @param exception 异常
     * @author Moyuyanli
     * @date 2022/12/10 0:55
     */
    public static void debug(Exception exception) {
        log.debug(name + exception);
    }


    public static void setName(String name) {
        Log.name = name;
    }
}
