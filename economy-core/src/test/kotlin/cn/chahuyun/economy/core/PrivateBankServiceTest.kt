package cn.chahuyun.economy.core

import cn.chahuyun.economy.model.privatebank.*
import cn.chahuyun.economy.privatebank.PrivateBankBankruptcyService
import cn.chahuyun.economy.privatebank.PrivateBankDebtService
import cn.chahuyun.economy.privatebank.PrivateBankLedger
import cn.chahuyun.economy.privatebank.PrivateBankService
import java.util.*
import kotlin.test.*

class PrivateBankServiceTest {

    @Test
    fun `liquidity loan freeze plan moves amount once into inventory`() {
        val plan = PrivateBankService.buildLoanFundFreezePlan("LIQUIDITY", 10_000_000.0)

        assertEquals("LIQUIDITY", plan?.source)
        assertEquals(
            listOf(
                PrivateBankService.LoanFundMove(PrivateBankLedger.LIQUIDITY_DESC, -10_000_000.0),
                PrivateBankService.LoanFundMove(PrivateBankLedger.INVENTORY_DESC, 10_000_000.0)
            ),
            plan?.moves
        )
    }

    @Test
    fun `owner loan freeze plan only credits inventory after wallet debit`() {
        val plan = PrivateBankService.buildLoanFundFreezePlan("owner", 20_000.0)

        assertEquals("OWNER", plan?.source)
        assertEquals(
            listOf(PrivateBankService.LoanFundMove(PrivateBankLedger.INVENTORY_DESC, 20_000.0)),
            plan?.moves
        )
    }

    @Test
    fun `unknown loan fund source is rejected`() {
        assertNull(PrivateBankService.buildLoanFundFreezePlan("vault", 1.0))
    }

    @Test
    fun `bank owner cannot deposit into own bank`() {
        val bank = PrivateBankDto(code = "teafox", ownerQq = 572490972)

        assertFalse(PrivateBankService.canUserDepositToBank(572490972, bank))
        assertTrue(PrivateBankService.canUserDepositToBank(10001, bank))
    }

    @Test
    fun `funding prefers a sufficient wallet over main bank`() {
        assertEquals(
            PrivateBankService.FundingSource.WALLET,
            PrivateBankService.selectFundingSource(100.0, walletBalance = 100.0, mainBankBalance = 1_000.0)
        )
    }

    @Test
    fun `funding falls back to main bank without combining balances`() {
        assertEquals(
            PrivateBankService.FundingSource.MAIN_BANK,
            PrivateBankService.selectFundingSource(100.0, walletBalance = 99.0, mainBankBalance = 100.0)
        )
        assertNull(
            PrivateBankService.selectFundingSource(100.0, walletBalance = 60.0, mainBankBalance = 40.0)
        )
    }

    @Test
    fun `funding pool transfer token uses source then destination`() {
        assertEquals(
            PrivateBankService.FundingPoolTransfer(
                PrivateBankService.FundingPool.RESERVE,
                PrivateBankService.FundingPool.LIQUIDITY
            ),
            PrivateBankService.parseFundingPoolTransfer("pf")
        )
        assertEquals(
            PrivateBankService.FundingPoolTransfer(
                PrivateBankService.FundingPool.LIQUIDITY,
                PrivateBankService.FundingPool.RESERVE
            ),
            PrivateBankService.parseFundingPoolTransfer("FP")
        )
        assertNull(PrivateBankService.parseFundingPoolTransfer("PP"))
        assertNull(PrivateBankService.parseFundingPoolTransfer("P"))
    }

    @Test
    fun `loan interest is limited to one through eighteen percent`() {
        assertFalse(PrivateBankService.isLoanInterestAllowed(9))
        assertTrue(PrivateBankService.isLoanInterestAllowed(10))
        assertTrue(PrivateBankService.isLoanInterestAllowed(180))
        assertFalse(PrivateBankService.isLoanInterestAllowed(181))
    }

    @Test
    fun `loan interest text trims whole percent decimals`() {
        assertEquals("5%", PrivateBankService.loanInterestPercentText(50))
        assertEquals("5.5%", PrivateBankService.loanInterestPercentText(55))
    }

    @Test
    fun `loan due total uses loan period rate once`() {
        assertEquals(1_160_000.0, PrivateBankService.calculateLoanDueTotal(1_000_000.0, 160))
        assertEquals(1_080_000.0, PrivateBankService.calculateLoanDueTotal(1_000_000.0, 80))
    }

    @Test
    fun `best loan offer prefers lower interest`() {
        val highRate = PrivateBankLoanOfferDto(
            id = 1,
            bankCode = "fox",
            remaining = 10_000_000.0,
            interest = 160,
            createdAt = 1L
        )
        val lowRate = PrivateBankLoanOfferDto(
            id = 2,
            bankCode = "fox",
            remaining = 10_000_000.0,
            interest = 80,
            createdAt = 2L
        )

        assertSame(lowRate, PrivateBankService.selectBestLoanOffer(listOf(highRate, lowRate), 1_000_000.0))
    }

    @Test
    fun `cancel loan offer returns remaining inventory to liquidity`() {
        assertEquals(
            listOf(
                PrivateBankService.LoanFundMove(PrivateBankLedger.INVENTORY_DESC, -7_000_000.0),
                PrivateBankService.LoanFundMove(PrivateBankLedger.LIQUIDITY_DESC, 7_000_000.0)
            ),
            PrivateBankService.buildLoanOfferCancelMoves(7_000_000.0)
        )
        assertEquals(emptyList(), PrivateBankService.buildLoanOfferCancelMoves(0.0))
    }

    @Test
    fun `gov bond maturity requires full lock days`() {
        val now = Date(1_000_000_000L)
        val twoDays = 2L * 24L * 60L * 60L * 1000L
        val matureHolding = PrivateBankGovBondHoldingDto(
            boughtAt = now.time - twoDays,
            lockDays = 2
        )
        val earlyHolding = PrivateBankGovBondHoldingDto(
            boughtAt = now.time - twoDays + 1L,
            lockDays = 2
        )

        assertTrue(PrivateBankService.isBondMatured(matureHolding, now))
        assertFalse(PrivateBankService.isBondMatured(earlyHolding, now))
    }

    @Test
    fun `gov bond becomes unavailable with twenty four hours until redemption`() {
        val now = Date(2_000_000_000L)
        val issue = PrivateBankGovBondIssueDto(
            createdAt = now.time,
            lockDays = 3,
            remaining = 1_000_000.0
        )

        assertTrue(PrivateBankService.isBondIssueAvailable(issue, Date(now.time + 47L * 60L * 60L * 1000L)))
        assertFalse(PrivateBankService.isBondIssueAvailable(issue, Date(now.time + 48L * 60L * 60L * 1000L)))
    }

    @Test
    fun `withdraw reclaim uses only enabled remaining offers from high interest to low`() {
        val offers = listOf(
            PrivateBankLoanOfferDto(id = 1, remaining = 50.0, interest = 30, createdAt = 3),
            PrivateBankLoanOfferDto(id = 2, remaining = 40.0, interest = 80, createdAt = 2),
            PrivateBankLoanOfferDto(id = 3, remaining = 30.0, interest = 80, createdAt = 1),
            PrivateBankLoanOfferDto(id = 4, remaining = 1_000.0, interest = 180, enabled = false),
        )

        assertEquals(
            listOf(
                PrivateBankService.LoanOfferReclaim(3, 30.0),
                PrivateBankService.LoanOfferReclaim(2, 40.0),
            ),
            PrivateBankService.buildLoanOfferReclaimPlan(offers, inventory = 70.0, requested = 100.0)
        )
    }

    @Test
    fun `withdraw reclaim is capped by real inventory`() {
        val offers = listOf(PrivateBankLoanOfferDto(id = 1, remaining = 500.0, interest = 100))

        assertEquals(
            listOf(PrivateBankService.LoanOfferReclaim(1, 25.0)),
            PrivateBankService.buildLoanOfferReclaimPlan(offers, inventory = 25.0, requested = 100.0)
        )
    }

    @Test
    fun `main bank emergency debt accrues two percent simple daily interest`() {
        val start = 1_000_000L
        val debt = PrivateBankMainBankDebtDto(principal = 1_000.0, lastAccruedAt = start)

        assertTrue(PrivateBankDebtService.applyAccrual(debt, start + 2 * PrivateBankDebtService.DAY_MILLIS))
        assertEquals(40.0, debt.accruedInterest)
        assertTrue(PrivateBankDebtService.applyAccrual(debt, start + 3 * PrivateBankDebtService.DAY_MILLIS))
        assertEquals(60.0, debt.accruedInterest)
    }

    @Test
    fun `main bank emergency debt ignores incomplete days`() {
        val start = 1_000_000L
        val debt = PrivateBankMainBankDebtDto(principal = 1_000.0, lastAccruedAt = start)

        assertFalse(PrivateBankDebtService.applyAccrual(debt, start + PrivateBankDebtService.DAY_MILLIS - 1))
        assertEquals(0.0, debt.accruedInterest)
    }

    @Test
    fun `main bank emergency debt repayment pays interest before principal`() {
        val debt = PrivateBankMainBankDebtDto(principal = 1_000.0, accruedInterest = 100.0)

        val result = PrivateBankDebtService.applyRepayment(debt, 150.0, now = 123L)

        assertEquals(100.0, result.paidInterest)
        assertEquals(50.0, result.paidPrincipal)
        assertEquals(950.0, result.remainingDebt)
        assertEquals(0.0, debt.accruedInterest)
        assertEquals(950.0, debt.principal)
    }

    @Test
    fun `main bank emergency debt records full repayment time`() {
        val debt = PrivateBankMainBankDebtDto(principal = 100.0, accruedInterest = 2.0)

        val result = PrivateBankDebtService.applyRepayment(debt, 120.0, now = 456L)

        assertEquals(18.0, result.remainingInput)
        assertEquals(0.0, result.remainingDebt)
        assertEquals(456L, debt.repaidAt)
    }

    @Test
    fun `defaulter star is base star minus two with one star floor`() {
        assertEquals(5, PrivateBankService.calculateStar(10_000_000.0, 1.0, 5.0, defaulter = false))
        assertEquals(3, PrivateBankService.calculateStar(10_000_000.0, 1.0, 5.0, defaulter = true))
        assertEquals(2, PrivateBankService.calculateStar(0.0, 1.0, 0.0, defaulter = false))
        assertEquals(1, PrivateBankService.calculateStar(0.0, 1.0, 0.0, defaulter = true))
    }

    @Test
    fun `bankruptcy requires debt strictly greater than one billion`() {
        assertFalse(PrivateBankBankruptcyService.isOverThreshold(1_000_000_000.0))
        assertTrue(PrivateBankBankruptcyService.isOverThreshold(1_000_000_000.1))
    }

    @Test
    fun `bankruptcy transfers remaining debt equally to owner accounts`() {
        assertEquals(
            500_000_001.0 to 500_000_001.0,
            PrivateBankBankruptcyService.ownerDebtShares(1_000_000_002.0)
        )
        assertEquals(
            50.1 to 50.1,
            PrivateBankBankruptcyService.ownerDebtShares(100.2)
        )
    }

    @Test
    fun `bankruptcy voids outstanding borrower loan`() {
        val loan = PrivateBankLoanDto(principal = 1_000.0, dueTotal = 1_180.0, repaidAmount = 200.0)

        assertTrue(PrivateBankBankruptcyService.voidLoan(loan, now = 999L))
        assertEquals(200.0, loan.dueTotal)
        assertEquals(999L, loan.repaidAt)
        assertFalse(PrivateBankBankruptcyService.voidLoan(loan, now = 1_000L))
    }
}
