package cn.chahuyun.economy.service

import cn.chahuyun.economy.entity.farm.FarmPlayer
import java.time.LocalDate

object FarmSocialQuotaService {
    const val REQUIRED_LEVEL = 13
    private const val ADVANCED_LEVEL = 18
    private const val DEFAULT_DAILY_LIMIT = 5
    private const val ADVANCED_DAILY_LIMIT = 10

    fun dailyLimit(level: Int): Int = when {
        level < REQUIRED_LEVEL -> 0
        level >= ADVANCED_LEVEL -> ADVANCED_DAILY_LIMIT
        else -> DEFAULT_DAILY_LIMIT
    }

    fun todayCount(player: FarmPlayer, today: LocalDate = LocalDate.now()): Int =
        if (player.lastWaterDate == today.toString()) player.todayWaterCount else 0

    fun refresh(player: FarmPlayer, today: LocalDate = LocalDate.now()) {
        val date = today.toString()
        if (player.lastWaterDate != date) {
            player.lastWaterDate = date
            player.todayWaterCount = 0
        }
    }

    fun remaining(player: FarmPlayer): Int =
        (dailyLimit(player.level) - player.todayWaterCount).coerceAtLeast(0)

    fun consume(player: FarmPlayer, count: Int = 1): Boolean {
        require(count > 0) { "count must be positive" }
        refresh(player)
        if (remaining(player) < count) return false
        player.todayWaterCount += count
        return true
    }
}
