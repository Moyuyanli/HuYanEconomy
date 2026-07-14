package cn.chahuyun.economy.data.converter

import cn.chahuyun.economy.converter.v1.PrivateBankMainBankDebtV1Converter
import cn.chahuyun.economy.converter.v2.PrivateBankMainBankDebtV2Converter
import cn.chahuyun.economy.model.privatebank.PrivateBankMainBankDebtDto
import kotlin.test.Test
import kotlin.test.assertEquals

class PrivateBankMainBankDebtConverterTest {
    private val dto = PrivateBankMainBankDebtDto(
        id = 7,
        bankCode = "fox",
        principal = 150_000_000.0,
        accruedInterest = 3_000_000.0,
        lastAccruedAt = 100,
        createdAt = 90,
        updatedAt = 110,
        repaidAt = 0,
    )

    @Test
    fun `v1 debt converter round trips all accounting fields`() {
        assertEquals(dto, PrivateBankMainBankDebtV1Converter().toDto(PrivateBankMainBankDebtV1Converter().toEntity(dto)))
    }

    @Test
    fun `v2 debt converter round trips all accounting fields`() {
        assertEquals(dto, PrivateBankMainBankDebtV2Converter().toDto(PrivateBankMainBankDebtV2Converter().toEntity(dto)))
    }
}
