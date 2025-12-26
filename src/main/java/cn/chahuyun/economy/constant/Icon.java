package cn.chahuyun.economy.constant;

import cn.chahuyun.economy.EconomyBuildConstants;
import net.mamoe.mirai.utils.MiraiLogger;

/**
 * @author Moyuyanli
 * @date 2024/8/15 16:26
 */
public class Icon {

    private static final String icon1 = "  _    _    __     __         ______                                      ";
    private static final String icon2 = " | |  | |   \\ \\   / /        |  ____|                                     ";
    private static final String icon3 = " | |__| |_   \\ \\_/ /_ _ _ __ | |__   ___ ___  _ __   ___  _ __ ___  _   _ ";
    private static final String icon4 = " |  __  | | | \\   / _` | '_ \\|  __| / __/ _ \\| '_ \\ / _ \\| '_ ` _ \\| | | |";
    private static final String icon5 = " | |  | | |_| || | (_| | | | | |___| (_| (_) | | | | (_) | | | | | | |_| |";
    private static final String icon6 = " |_|  |_|\\__,_||_|\\__,_|_| |_|______\\___\\___/|_| |_|\\___/|_| |_| |_|\\__, |";
    private static final String icon7 = "                                                                     __/ |";
    private static final String icon8 = "                                                                    |___/ ";
    private static final String icon9 = "                                                              v " + EconomyBuildConstants.VERSION;

    /**
     * 打印log
     *
     * @param logger 日志
     */
    public static void init(MiraiLogger logger) {
        logger.info(icon1);
        logger.info(icon2);
        logger.info(icon3);
        logger.info(icon4);
        logger.info(icon5);
        logger.info(icon6);
        logger.info(icon7);
        logger.info(icon8);
        logger.info(icon9);
    }

}
