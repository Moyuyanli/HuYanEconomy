package cn.chahuyun.economy.core

import cn.chahuyun.economy.feature.sign.SignService
import cn.chahuyun.economy.model.props.PropsCard
import java.util.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 签到对内领域服务的单元测试。
 *
 * 只覆盖**纯计算/纯判断**部分（不需要数据库）：月卡有效期判定是 `signProp` 里最容易写错的
 * 一处分支——月卡被动生效、不要求 `status = true`，因此单独锁死行为。
 */
class SignServiceTest {

    @Test
    fun `monthly card is passive and does not require activation`() {
        val card = PropsCard(code = PropsCard.MONTHLY).apply {
            status = false
            canItExpire = true
            expiredTime = Date(System.currentTimeMillis() + 60_000L)
        }

        assertTrue(SignService.isMonthlyCardActive(card))
    }

    @Test
    fun `expired monthly card is inactive`() {
        val card = PropsCard(code = PropsCard.MONTHLY).apply {
            status = true
            canItExpire = true
            expiredTime = Date(System.currentTimeMillis() - 1L)
        }

        assertFalse(SignService.isMonthlyCardActive(card))
    }
}
