package cn.chahuyun.economy.service

import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.user.UserRaffleDto
import java.util.concurrent.ConcurrentHashMap

object LuckyDrawService {

    private const val SINGLE_COOLDOWN = 60_000L
    private const val TEN_COOLDOWN = 600_000L

    private val singleCooldownMap = ConcurrentHashMap<Long, Long>()
    private val tenCooldownMap = ConcurrentHashMap<Long, Long>()

    fun takeUserRaffle(userId: Long): UserRaffleDto =
        userRaffleProxy.findById(userId) ?: userRaffleProxy.save(UserRaffleDto(id = userId))

    fun checkSingleCooldown(userId: Long): Boolean =
        System.currentTimeMillis() - (singleCooldownMap[userId] ?: 0L) >= SINGLE_COOLDOWN

    fun checkTenCooldown(userId: Long): Boolean =
        System.currentTimeMillis() - (tenCooldownMap[userId] ?: 0L) >= TEN_COOLDOWN

    fun singleRemainingSeconds(userId: Long): Int {
        val lastTime = singleCooldownMap[userId] ?: 0L
        val remainingMs = SINGLE_COOLDOWN - (System.currentTimeMillis() - lastTime)
        return (remainingMs.coerceAtLeast(0) / 1000).toInt()
    }

    fun tenRemainingSeconds(userId: Long): Int {
        val lastTime = tenCooldownMap[userId] ?: 0L
        val remainingMs = TEN_COOLDOWN - (System.currentTimeMillis() - lastTime)
        return (remainingMs.coerceAtLeast(0) / 1000).toInt()
    }

    fun updateCooldown(userId: Long, isTen: Boolean = false) {
        val now = System.currentTimeMillis()
        if (isTen) tenCooldownMap[userId] = now else singleCooldownMap[userId] = now
    }

    private val userRaffleProxy
        get() = EntityProxyRegistry.get<UserRaffleDto>("user_raffle") ?: error("用户抽奖代理器未初始化")
}
