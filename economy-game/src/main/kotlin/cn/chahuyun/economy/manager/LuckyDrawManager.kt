package cn.chahuyun.economy.manager

import cn.chahuyun.economy.model.user.UserRaffleDto
import cn.chahuyun.economy.service.LuckyDrawService

/**
 * 抽奖相关兼容 facade。
 */
object LuckyDrawManager {
    @JvmStatic
    fun take(userId: Long): UserRaffleDto =
        LuckyDrawService.takeUserRaffle(userId)

    @JvmStatic
    fun checkSingleCooldown(userId: Long): Boolean =
        LuckyDrawService.checkSingleCooldown(userId)

    @JvmStatic
    fun checkTenCooldown(userId: Long): Boolean =
        LuckyDrawService.checkTenCooldown(userId)

    @JvmStatic
    fun singleRemainingSeconds(userId: Long): Int =
        LuckyDrawService.singleRemainingSeconds(userId)

    @JvmStatic
    fun tenRemainingSeconds(userId: Long): Int =
        LuckyDrawService.tenRemainingSeconds(userId)

    @JvmStatic
    fun updateCooldown(userId: Long, isTen: Boolean = false) =
        LuckyDrawService.updateCooldown(userId, isTen)
}


