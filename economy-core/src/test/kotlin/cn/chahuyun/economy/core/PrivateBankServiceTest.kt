package cn.chahuyun.economy.core

import cn.chahuyun.economy.model.privatebank.PrivateBankDto
import cn.chahuyun.economy.model.privatebank.PrivateBankGovBondHoldingDto
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
    fun `loan interest is limited to one through eighteen percent`() {
        assertFalse(PrivateBankService.isLoanInterestAllowed(9))
        assertTrue(PrivateBankService.isLoanInterestAllowed(10))
        assertTrue(PrivateBankService.isLoanInterestAllowed(180))
        assertFalse(PrivateBankService.isLoanInterestAllowed(181))
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
}
