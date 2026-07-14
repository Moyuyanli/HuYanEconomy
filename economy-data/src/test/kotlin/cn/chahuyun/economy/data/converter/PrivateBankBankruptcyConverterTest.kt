package cn.chahuyun.economy.data.converter

import cn.chahuyun.economy.converter.v1.PrivateBankV1Converter
import cn.chahuyun.economy.converter.v2.PrivateBankV2Converter
import cn.chahuyun.economy.model.privatebank.PrivateBankDto
import kotlin.test.Test
import kotlin.test.assertEquals

class PrivateBankBankruptcyConverterTest {
    private val dto = PrivateBankDto(
        id = 3,
        code = "closed-bank",
        name = "已破产银行",
        ownerQq = 10001,
        createdAt = 100,
        star = 1,
        bankruptAt = 200,
    )

    @Test
    fun `v1 converter preserves permanent bankruptcy time`() {
        assertEquals(dto, PrivateBankV1Converter().toDto(PrivateBankV1Converter().toEntity(dto)))
    }

    @Test
    fun `v2 converter preserves permanent bankruptcy time`() {
        assertEquals(dto, PrivateBankV2Converter().toDto(PrivateBankV2Converter().toEntity(dto)))
    }
}
