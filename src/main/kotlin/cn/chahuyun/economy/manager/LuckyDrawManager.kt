package cn.chahuyun.economy.manager

import cn.chahuyun.economy.entity.UserRaffle
import cn.chahuyun.economy.repository.UserRaffleRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * 抽奖相关的“非事件监听”逻辑。
 *
 * 说明：
 * - `action.LuckyDrawAction` 仅保留指令入口与消息发送。
 * - 冷却、用户抽奖统计数据的获取等可复用逻辑下沉到这里。
 */
object LuckyDrawManager {

    // 单抽冷却时间(60秒)
    private const val SINGLE_COOLDOWN = 60_000L

    // 十连冷却时间(600秒)
    private const val TEN_COOLDOWN = 600_000L

    // 用户冷却记录 (userId -> 上次抽奖时间戳)
    private val singleCooldownMap = ConcurrentHashMap<Long, Long>()
    private val tenCooldownMap = ConcurrentHashMap<Long, Long>()

    @JvmStatic
    fun take(userId: Long): UserRaffle {
        val userRaffle = UserRaffleRepository.findById(userId)
        if (userRaffle != null) return userRaffle
        return UserRaffleRepository.save(UserRaffle(userId))
    }

    @JvmStatic
    fun checkSingleCooldown(userId: Long): Boolean {
        val lastTime = singleCooldownMap[userId] ?: 0L
        return System.currentTimeMillis() - lastTime >= SINGLE_COOLDOWN
    }

    @JvmStatic
    fun checkTenCooldown(userId: Long): Boolean {
        val lastTime = tenCooldownMap[userId] ?: 0L
        return System.currentTimeMillis() - lastTime >= TEN_COOLDOWN
    }

    @JvmStatic
    fun singleRemainingSeconds(userId: Long): Int {
        val lastTime = singleCooldownMap[userId] ?: 0L
        val remainingMs = SINGLE_COOLDOWN - (System.currentTimeMillis() - lastTime)
        return (remainingMs.coerceAtLeast(0) / 1000).toInt()
    }

    @JvmStatic
    fun tenRemainingSeconds(userId: Long): Int {
        val lastTime = tenCooldownMap[userId] ?: 0L
        val remainingMs = TEN_COOLDOWN - (System.currentTimeMillis() - lastTime)
        return (remainingMs.coerceAtLeast(0) / 1000).toInt()
    }

    @JvmStatic
    fun updateCooldown(userId: Long, isTen: Boolean = false) {
        val now = System.currentTimeMillis()
        if (isTen) tenCooldownMap[userId] = now else singleCooldownMap[userId] = now
    }
}


