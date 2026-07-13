package cn.chahuyun.economy.core

import cn.chahuyun.economy.model.privatebank.PrivateBankDto
import cn.chahuyun.economy.usecase.PrivateBankUsecase
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrivateBankUsecaseTest {

    @Test
    fun `bank list item includes key bank information`() {
        val item = PrivateBankUsecase.PrivateBankListItem(
            bank = PrivateBankDto(
                code = "teafox",
                name = "小狐狸的银行",
                slogan = "稳定兑付，谨慎放贷",
                ownerQq = 572490972,
                depositorInterest = 1,
                withdrawRequests = 10,
                withdrawFailures = 1,
                star = 3,
                avgReview = 4.25
            ),
            owner = "Kemomimi(572490972)",
            totalDeposit = 10_000.0,
            remainingLoanLimit = 10_000_000.0,
            outstandingLoan = 2_500_000.0,
            inventory = 20_000_000.0,
            withdrawSuccessRate = 90.0
        )

        val text = PrivateBankUsecase.formatBankListItem(1, item)

        assertContains(text, "#1  小狐狸的银行  code=teafox")
        assertContains(text, "行长：Kemomimi(572490972)")
        assertContains(text, "星级：★★★（3） / 评分：4.25")
        assertContains(text, "存款利率：0.1% / 取款成功率：90.0%")
        assertContains(text, "存款总额：10000.0 / 可借额度：10M")
        assertContains(text, "放贷库存：20M / 待收本息：2.5M")
        assertContains(text, "失信：否")
        assertContains(text, "描述：稳定兑付，谨慎放贷")
    }

    @Test
    fun `loan offer command dedup rejects duplicate within window`() {
        assertTrue(PrivateBankUsecase.markLoanOfferCommandIfFresh("572490972|teafox|10M", now = 10_000L))
        assertFalse(PrivateBankUsecase.markLoanOfferCommandIfFresh("572490972|teafox|10M", now = 12_000L))
        assertTrue(PrivateBankUsecase.markLoanOfferCommandIfFresh("572490972|teafox|10M", now = 16_000L))
    }
}
