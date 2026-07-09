package cn.chahuyun.economy.common

import cn.chahuyun.economy.utils.MoneyFormatUtil
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyFormatUtilTest {

    @Test
    fun `format keeps one decimal before million`() {
        assertEquals("0.0", MoneyFormatUtil.format(0.0))
        assertEquals("1.0", MoneyFormatUtil.format(1.0))
        assertEquals("999999.0", MoneyFormatUtil.format(999_999.0))
    }

    @Test
    fun `format uses compact thousand-based suffixes`() {
        assertEquals("1M", MoneyFormatUtil.format(1_000_000.0))
        assertEquals("52.3M", MoneyFormatUtil.format(52_300_000.0))
        assertEquals("999.9M", MoneyFormatUtil.format(999_900_000.0))
        assertEquals("1G", MoneyFormatUtil.format(1_000_000_000.0))
        assertEquals("999.9G", MoneyFormatUtil.format(999_900_000_000.0))
        assertEquals("1T", MoneyFormatUtil.format(1_000_000_000_000.0))
        assertEquals("1P", MoneyFormatUtil.format(1_000_000_000_000_000.0))
    }

    @Test
    fun `parse accepts compact money text`() {
        assertEquals(52_300_000.0, MoneyFormatUtil.parse("52.3M"))
        assertEquals(20_000.0, MoneyFormatUtil.parse("20K"))
        assertEquals(1_000_000_000.0, MoneyFormatUtil.parse("1G"))
        assertEquals(10_000.0, MoneyFormatUtil.parse("1万"))
        assertEquals(100_000_000.0, MoneyFormatUtil.parse("1亿"))
    }
}
