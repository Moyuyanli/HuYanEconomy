package cn.chahuyun.economy.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserSignRepairTest {

    @Test
    fun `restores larger legacy streak without rolling back latest sign time`() {
        val merged = mergeUserSignData(
            legacySign = true,
            legacySignTime = 100,
            legacySignNumber = 120,
            legacyOldSignNumber = 30,
            currentSign = true,
            currentSignTime = 200,
            currentSignNumber = 1,
            currentOldSignNumber = 0
        )

        assertEquals(200, merged.signTime)
        assertEquals(120, merged.signNumber)
        assertEquals(30, merged.oldSignNumber)
        assertTrue(merged.changed)
    }

    @Test
    fun `uses sign state belonging to latest sign time`() {
        val merged = mergeUserSignData(
            legacySign = true,
            legacySignTime = 300,
            legacySignNumber = 8,
            legacyOldSignNumber = 0,
            currentSign = false,
            currentSignTime = 200,
            currentSignNumber = 8,
            currentOldSignNumber = 0
        )

        assertTrue(merged.sign)
        assertEquals(300, merged.signTime)
        assertTrue(merged.changed)
    }

    @Test
    fun `leaves newer complete current data unchanged`() {
        val merged = mergeUserSignData(
            legacySign = false,
            legacySignTime = 100,
            legacySignNumber = 5,
            legacyOldSignNumber = 3,
            currentSign = true,
            currentSignTime = 200,
            currentSignNumber = 9,
            currentOldSignNumber = 4
        )

        assertTrue(merged.sign)
        assertEquals(200, merged.signTime)
        assertEquals(9, merged.signNumber)
        assertEquals(4, merged.oldSignNumber)
        assertFalse(merged.changed)
    }
}
