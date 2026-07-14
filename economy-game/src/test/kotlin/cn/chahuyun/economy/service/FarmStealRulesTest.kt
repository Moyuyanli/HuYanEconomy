package cn.chahuyun.economy.service

import cn.chahuyun.economy.constant.FarmConstants
import cn.chahuyun.economy.entity.farm.FarmCrop
import cn.chahuyun.economy.entity.farm.FarmPlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FarmStealRulesTest {

    @Test
    fun `success rate follows asymmetric level curve`() {
        val expected = mapOf(
            -10 to 2,
            -7 to 2,
            -1 to 8,
            0 to 20,
            1 to 15,
            2 to 6,
            3 to 0,
            10 to 0,
        )

        expected.forEach { (delta, rate) ->
            assertEquals(rate, FarmStealRules.successRate(20 + delta, 20), "delta=$delta")
        }
    }

    @Test
    fun `crop must be level twelve mature planted and not stolen this season`() {
        val now = 10_000L
        val plot = plantedPlot(currentSeason = 2, nextMatureAt = now)
        val levelEleven = crop(level = 11, yield = 1)
        val levelTwelve = crop(level = 12, yield = 2)

        assertFalse(FarmStealRules.isStealable(plot, levelEleven, now))
        assertTrue(FarmStealRules.isStealable(plot, levelTwelve, now))

        plot.nextMatureAt = now + 1
        assertFalse(FarmStealRules.isStealable(plot, levelTwelve, now))

        plot.nextMatureAt = now
        plot.stolenSeason = plot.currentSeason
        plot.stolenAmount = 1
        assertFalse(FarmStealRules.isStealable(plot, levelTwelve, now))

        plot.currentSeason += 1
        assertTrue(FarmStealRules.isStealable(plot, levelTwelve, now))

        plot.status = FarmConstants.PLOT_EMPTY
        assertFalse(FarmStealRules.isStealable(plot, levelTwelve, now))
    }

    @Test
    fun `steal amount respects direction percentages and owner reserve`() {
        assertEquals(1, FarmStealRules.stealAmount(2, 20, 20) { it })
        assertEquals(2, FarmStealRules.stealAmount(5, 20, 18) { it })
        assertEquals(2, FarmStealRules.stealAmount(10, 22, 21) { it })
        assertEquals(3, FarmStealRules.stealAmount(15, 18, 25) { it })
        assertEquals(1, FarmStealRules.stealAmount(2, 18, 25) { it })
    }

    @Test
    fun `owner harvest loses fruit only in stolen season`() {
        val crop = crop(level = 20, yield = 8)
        val plot = plantedPlot(currentSeason = 2, nextMatureAt = 0).apply {
            stolenSeason = 2
            stolenAmount = 3
        }

        assertEquals(5, FarmStealRules.ownerHarvestAmount(plot, crop))
        plot.currentSeason = 3
        assertEquals(8, FarmStealRules.ownerHarvestAmount(plot, crop))

        plot.stolenSeason = 3
        plot.stolenAmount = 99
        assertEquals(1, FarmStealRules.ownerHarvestAmount(plot, crop))
    }

    private fun plantedPlot(currentSeason: Int, nextMatureAt: Long): FarmPlot = FarmPlot().apply {
        status = FarmConstants.PLOT_PLANTED
        this.currentSeason = currentSeason
        this.nextMatureAt = nextMatureAt
    }

    private fun crop(level: Int, yield: Int): FarmCrop = FarmCrop().apply {
        this.level = level
        yieldPerSeason = yield
    }
}
