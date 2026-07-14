package cn.chahuyun.economy.service

import kotlin.test.Test
import kotlin.test.assertEquals

class FishingRuntimeServiceTest {

    @Test
    fun `red eyes reduces fishing cooldown by eighty percent`() {
        assertEquals(9 * 60_000L, FishingRuntimeService.calculateCooldownMillis(9, false))
        assertEquals(108_000L, FishingRuntimeService.calculateCooldownMillis(9, true))
    }

    @Test
    fun `cooldown display rounds partial minutes up`() {
        assertEquals(0L, FishingRuntimeService.minutesRoundedUp(0L))
        assertEquals(1L, FishingRuntimeService.minutesRoundedUp(1L))
        assertEquals(2L, FishingRuntimeService.minutesRoundedUp(60_001L))
    }
}
