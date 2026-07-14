package cn.chahuyun.economy.data.converter

import cn.chahuyun.economy.converter.v2.PrivateBankGovBondIssueV2Converter
import cn.chahuyun.economy.entity.v2.privatebank.PrivateBankGovBondIssueEntity
import kotlin.test.Test
import kotlin.test.assertEquals

class PrivateBankGovBondIssueV2ConverterTest {

    @Test
    fun `legacy null code falls back to week key`() {
        val entity = PrivateBankGovBondIssueEntity(weekKey = "GB-LEGACY-001")
        PrivateBankGovBondIssueEntity::class.java.getDeclaredField("code").apply {
            isAccessible = true
            set(entity, null)
        }

        assertEquals("GB-LEGACY-001", PrivateBankGovBondIssueV2Converter().toDto(entity).code)
    }
}
