package cn.chahuyun.economy.core

import cn.chahuyun.economy.manager.SignManager
import cn.chahuyun.economy.model.props.PropsCard
import java.util.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignManagerTest {

    @Test
    fun `monthly card is passive and does not require activation`() {
        val card = PropsCard(code = PropsCard.MONTHLY).apply {
            status = false
            canItExpire = true
            expiredTime = Date(System.currentTimeMillis() + 60_000L)
        }

        assertTrue(SignManager.isMonthlyCardActive(card))
    }

    @Test
    fun `expired monthly card is inactive`() {
        val card = PropsCard(code = PropsCard.MONTHLY).apply {
            status = true
            canItExpire = true
            expiredTime = Date(System.currentTimeMillis() - 1L)
        }

        assertFalse(SignManager.isMonthlyCardActive(card))
    }
}
