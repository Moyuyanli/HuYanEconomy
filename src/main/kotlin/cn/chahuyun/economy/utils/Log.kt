package cn.chahuyun.economy.utils

import cn.chahuyun.economy.HuYanEconomy
import net.mamoe.mirai.utils.MiraiLogger

/**
 * 日志
 * 使用此类日志做统一处理
 */
object Log {
    private val log: MiraiLogger = HuYanEconomy.logger

    @JvmField
    var name: String = "壶言经济--"

    @JvmStatic
    fun info(msg: String) {
        log.info(name + msg)
    }

    @JvmStatic
    fun warning(msg: String) {
        log.warning(name + msg)
    }

    @JvmStatic
    fun error(msg: String) {
        log.error(name + msg)
    }

    @JvmStatic
    fun error(e: Throwable) {
        log.error(name + (e.message ?: "unknown error"), e)
    }

    @JvmStatic
    fun error(msg: String, e: Throwable) {
        log.error(name + msg)
        log.error(name + (e.message ?: "unknown error"), e)
    }

    @JvmStatic
    fun debug(msg: String) {
        log.debug(name + msg)
    }

    @JvmStatic
    fun debug(exception: Exception) {
        log.debug(name + exception)
    }
}
