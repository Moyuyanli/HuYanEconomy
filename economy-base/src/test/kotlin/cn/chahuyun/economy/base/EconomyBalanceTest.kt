package cn.chahuyun.economy.base

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EconomyBalanceTest {

    @Test
    fun `more than two decimals are rounded with half up instead of rejected`() {
        assertEquals("1.01", EconomyBalance.of(BigDecimal("1.005")).format())
        assertEquals("1.00", EconomyBalance.of(BigDecimal("1.004")).format())
        assertEquals("2.00", EconomyBalance.of(BigDecimal("1.999")).format())
        assertEquals("0.01", EconomyBalance.of(BigDecimal("0.005")).format())
    }

    @Test
    fun `convenience factories normalise to two decimals`() {
        assertEquals("7.00", EconomyBalance.of(7).format())
        assertEquals("42.00", EconomyBalance.of(42L).format())
        assertEquals("3.50", EconomyBalance.of(3.5).format())
        assertEquals("0.30", EconomyBalance.of(0.1 + 0.2).format())
    }

    @Test
    fun `range check accepts decimal 19 2 boundary`() {
        assertTrue(EconomyBalance.of(BigDecimal("99999999999999999.99")).isWithinRange())
        assertTrue(EconomyBalance.MAX_ABS.compareTo(BigDecimal("99999999999999999.99")) == 0)

        assertFalse(EconomyBalance.of(BigDecimal("100000000000000000.00")).isWithinRange())
        assertFalse(EconomyBalance.of(BigDecimal("1000000000000000000000")).isWithinRange())
    }

    @Test
    fun `range check treats negative values by absolute value`() {
        assertTrue(EconomyBalance.of(BigDecimal("-99999999999999999.99")).isWithinRange())
        assertFalse(EconomyBalance.of(BigDecimal("-100000000000000000.00")).isWithinRange())
    }

    @Test
    fun `usable amount requires positive value inside range`() {
        assertTrue(EconomyBalance.of(1).isUsableAmount())
        assertTrue(EconomyBalance.of(EconomyBalance.MAX_ABS).isUsableAmount())

        assertFalse(EconomyBalance.of(0).isUsableAmount())
        assertFalse(EconomyBalance.of(-1).isUsableAmount())
        assertFalse(EconomyBalance.of(BigDecimal("100000000000000000.00")).isUsableAmount())
    }

    @Test
    fun `arithmetic keeps two decimal scale`() {
        assertEquals("3.00", (EconomyBalance.of(1) + EconomyBalance.of(2)).format())
        assertEquals("-1.00", (EconomyBalance.of(1) - EconomyBalance.of(2)).format())
        assertEquals("0.30", (EconomyBalance.of(BigDecimal("0.10")) + EconomyBalance.of(BigDecimal("0.20"))).format())
    }

    @Test
    fun `comparison follows numeric ordering`() {
        assertTrue(EconomyBalance.of(2) > EconomyBalance.of(1))
        assertEquals(0, EconomyBalance.of(1).compareTo(EconomyBalance.of(BigDecimal("1.00"))))
    }

    @Test
    fun `format honours the requested scale`() {
        assertEquals("1.50", EconomyBalance.of(BigDecimal("1.5")).format())
        assertEquals("1.5", EconomyBalance.of(BigDecimal("1.5")).format(1))
        assertEquals("2", EconomyBalance.of(BigDecimal("1.5")).format(0))
    }
}
