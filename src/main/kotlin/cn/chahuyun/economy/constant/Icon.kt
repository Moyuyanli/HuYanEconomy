package cn.chahuyun.economy.constant

import cn.chahuyun.economy.EconomyBuildConstants
import net.mamoe.mirai.utils.MiraiLogger

object Icon {
    private const val ICON_1 = "  _    _    __     __         ______                                      "
    private const val ICON_2 = " | |  | |   \\ \\   / /        |  ____|                                     "
    private const val ICON_3 = " | |__| |_   \\ \\_/ /_ _ _ __ | |__   ___ ___  _ __   ___  _ __ ___  _   _ "
    private const val ICON_4 = " |  __  | | | \\   / _` | '_ \\|  __| / __/ _ \\| '_ \\ / _ \\| '_ ` _ \\| | | |"
    private const val ICON_5 = " | |  | | |_| || | (_| | | | | |___| (_| (_) | | | | (_) | | | | | | |_| |"
    private const val ICON_6 = " |_|  |_|\\__,_||_|\\__,_|_| |_|______\\___\\___/|_| |_|\\___/|_| |_| |_|\\__, |"
    private const val ICON_7 = "                                                                     __/ |"
    private const val ICON_8 = "                                                                    |___/ "
    private val ICON_9 = "                                                              v ${EconomyBuildConstants.VERSION}"

    /**
     * 打印log
     */
    @JvmStatic
    fun init(logger: MiraiLogger) {
        logger.info(ICON_1)
        logger.info(ICON_2)
        logger.info(ICON_3)
        logger.info(ICON_4)
        logger.info(ICON_5)
        logger.info(ICON_6)
        logger.info(ICON_7)
        logger.info(ICON_8)
        logger.info(ICON_9)
    }
}
