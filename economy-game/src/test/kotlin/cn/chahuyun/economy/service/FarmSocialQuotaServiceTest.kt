package cn.chahuyun.economy.service

import cn.chahuyun.economy.entity.farm.FarmPlayer
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FarmSocialQuotaServiceTest {

    @Test
    fun `daily limit is shared by level`() {
        assertEquals(0, FarmSocialQuotaService.dailyLimit(12))
        assertEquals(5, FarmSocialQuotaService.dailyLimit(13))
        assertEquals(5, FarmSocialQuotaService.dailyLimit(17))
        assertEquals(10, FarmSocialQuotaService.dailyLimit(18))
        assertEquals(10, FarmSocialQuotaService.dailyLimit(30))
    }

    @Test
    fun `counter resets by date and cannot exceed limit`() {
        val today = LocalDate.of(2026, 7, 14)
        val player = FarmPlayer().apply {
            level = 13
            lastWaterDate = today.minusDays(1).toString()
            todayWaterCount = 5
        }

        assertEquals(0, FarmSocialQuotaService.todayCount(player, today))
        FarmSocialQuotaService.refresh(player, today)
        assertEquals(today.toString(), player.lastWaterDate)
        assertEquals(0, player.todayWaterCount)

        assertTrue(FarmSocialQuotaService.consume(player, 5))
        assertEquals(0, FarmSocialQuotaService.remaining(player))
        assertFalse(FarmSocialQuotaService.consume(player))
        assertEquals(5, player.todayWaterCount)
    }

    @Test
    fun `shield cost is five percent rounded up`() {
        assertEquals(7_000L, FarmShieldRules.activationCost(140_000L))
        assertEquals(6L, FarmShieldRules.activationCost(101L))
        assertEquals(12 * 60 * 60 * 1_000L, FarmShieldRules.DURATION_MILLIS)
    }
}
