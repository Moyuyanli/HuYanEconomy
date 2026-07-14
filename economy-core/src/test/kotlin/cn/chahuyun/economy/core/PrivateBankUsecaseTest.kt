package cn.chahuyun.economy.core

import cn.chahuyun.economy.model.privatebank.PrivateBankDto
import cn.chahuyun.economy.model.privatebank.PrivateBankLoanDto
import cn.chahuyun.economy.model.privatebank.PrivateBankLoanOfferDto
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
            withdrawSuccessRate = 90.0,
            minInterestOffer = PrivateBankUsecase.LoanOfferListItem(4_000_000.0, 15),
            maxInterestOffer = PrivateBankUsecase.LoanOfferListItem(6_000_000.0, 50),
        )

        val text = PrivateBankUsecase.formatBankListItem(1, item)

        assertContains(text, "#1  小狐狸的银行  code=teafox")
        assertContains(text, "行长：Kemomimi(572490972)")
        assertContains(text, "星级：★★★（3） / 评分：4.25")
        assertContains(text, "存款利率：0.1% / 取款成功率：90.0%")
        assertContains(text, "存款总额：10000.0 / 可借额度：10M")
        assertContains(text, "放贷库存：10M (可借贷)/ 20M(总库存) 待收本息：2.5M")
        assertContains(text, "放贷利息：4M(1.5%) / 6M(5%)")
        assertContains(text, "失信：否")
        assertContains(text, "描述：稳定兑付，谨慎放贷")
    }

    @Test
    fun `bank list does not duplicate a single loan offer`() {
        val offer = PrivateBankUsecase.LoanOfferListItem(10_000_000.0, 15)
        val item = PrivateBankUsecase.PrivateBankListItem(
            bank = PrivateBankDto(),
            owner = "owner",
            totalDeposit = 0.0,
            remainingLoanLimit = 10_000_000.0,
            outstandingLoan = 0.0,
            inventory = 10_000_000.0,
            withdrawSuccessRate = 100.0,
            minInterestOffer = offer,
            maxInterestOffer = offer,
        )

        assertTrue(PrivateBankUsecase.formatLoanOfferRange(item) == "10M(1.5%)")
    }

    @Test
    fun `loan offer command dedup rejects duplicate within window`() {
        assertTrue(PrivateBankUsecase.markLoanOfferCommandIfFresh("572490972|teafox|10M", now = 10_000L))
        assertFalse(PrivateBankUsecase.markLoanOfferCommandIfFresh("572490972|teafox|10M", now = 12_000L))
        assertTrue(PrivateBankUsecase.markLoanOfferCommandIfFresh("572490972|teafox|10M", now = 16_000L))
    }

    @Test
    fun `borrowed loan item includes repayment details`() {
        val loan = PrivateBankLoanDto(
            id = 12,
            bankCode = "teafox",
            principal = 100_000.0,
            dueTotal = 107_000.0,
            repaidAmount = 20_000.0,
            interest = 10,
            dueAt = 1_784_041_200_000L
        )

        val text = PrivateBankUsecase.formatBorrowedLoan(loan, "小狐狸的银行")

        assertContains(text, "#12 | 银行=小狐狸的银行(teafox) | 待还款")
        assertContains(text, "本金=100000.0 | 日利率=1.0% | 利息=7000.0")
        assertContains(text, "已还=20000.0 | 待还=87000.0")
        assertContains(text, "最迟还款=")
    }

    @Test
    fun `loan borrower line follows money detail style`() {
        val loan = PrivateBankLoanDto(principal = 10_000_000.0, dueTotal = 10_500_000.0)

        assertTrue(
            PrivateBankUsecase.formatLoanBorrower(loan, "小狐狸(10001)") ==
                "- 小狐狸(10001) 借贷 额度=10M 利息=500000.0 未还款"
        )
    }

    @Test
    fun `settled loans and withdrawn offers are hidden`() {
        val settled = PrivateBankLoanDto(offerId = 1, dueTotal = 100.0, repaidAmount = 100.0, repaidAt = 1L)
        val outstanding = PrivateBankLoanDto(offerId = 1, dueTotal = 100.0, repaidAmount = 20.0)
        val enabled = PrivateBankLoanOfferDto(id = 1, enabled = true, remaining = 0.0)
        val withdrawn = PrivateBankLoanOfferDto(id = 1, enabled = false, remaining = 100.0)

        assertFalse(PrivateBankUsecase.isOutstandingLoan(settled))
        assertTrue(PrivateBankUsecase.isOutstandingLoan(outstanding))
        assertTrue(PrivateBankUsecase.isVisibleLoanOffer(enabled, listOf(outstanding)))
        assertFalse(PrivateBankUsecase.isVisibleLoanOffer(withdrawn, listOf(outstanding)))
    }

    @Test
    fun `real borrowable amount is capped by inventory`() {
        val offers = listOf(
            PrivateBankLoanOfferDto(enabled = true, remaining = 30_000_000.0),
            PrivateBankLoanOfferDto(enabled = true, remaining = 20_000_000.0),
            PrivateBankLoanOfferDto(enabled = false, remaining = 100_000_000.0),
        )

        assertTrue(PrivateBankUsecase.calculateBorrowableAmount(offers, 40_000_000.0) == 40_000_000.0)
        assertTrue(PrivateBankUsecase.calculateBorrowableAmount(offers, 80_000_000.0) == 50_000_000.0)
    }
}
