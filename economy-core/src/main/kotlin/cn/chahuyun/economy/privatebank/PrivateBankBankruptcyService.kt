package cn.chahuyun.economy.privatebank

import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.data.repository.PrivateBankRepository
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.privatebank.PrivateBankDto
import cn.chahuyun.economy.model.privatebank.PrivateBankLoanDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.ShareUtils
import xyz.cssxsh.mirai.economy.EconomyService

object PrivateBankBankruptcyService {
    const val BANKRUPTCY_THRESHOLD = 1_000_000_000.0
    private const val EPSILON = 0.0001

    data class BankruptcyResult(
        val bankrupt: Boolean,
        val debtAtTrigger: Double = 0.0,
        val repaidFromPools: Double = 0.0,
        val transferredToOwner: Double = 0.0,
        val depositsWrittenOff: Double = 0.0,
        val loansVoided: Int = 0,
    )

    fun isOverThreshold(debt: Double): Boolean = debt > BANKRUPTCY_THRESHOLD

    fun ownerDebtShares(amount: Double): Pair<Double, Double> {
        val walletShare = ShareUtils.rounding(amount.coerceAtLeast(0.0) / 2.0)
        return walletShare to ShareUtils.rounding(amount.coerceAtLeast(0.0) - walletShare)
    }

    fun voidLoan(loan: PrivateBankLoanDto, now: Long): Boolean {
        if (loan.repaidAt != 0L) return false
        loan.dueTotal = loan.repaidAmount
        loan.repaidAt = now
        return true
    }

    fun evaluate(bankCode: String, now: Long = System.currentTimeMillis()): BankruptcyResult =
        PrivateBankLocks.withBankLock(bankCode) {
            val bank = PrivateBankRepository.findBankByCode(bankCode) ?: return@withBankLock BankruptcyResult(false)
            if (bank.isBankrupt()) return@withBankLock BankruptcyResult(true)
            val debt = PrivateBankDebtService.accrue(bankCode, now)
            val outstanding = PrivateBankDebtService.outstanding(debt)
            if (!isOverThreshold(outstanding)) return@withBankLock BankruptcyResult(false)
            liquidateLocked(bank, now)
        }

    fun processEligibleBanks(now: Long = System.currentTimeMillis()): Int {
        var count = 0
        PrivateBankRepository.listBanks().filterNot { it.isBankrupt() }.forEach { bank ->
            if (evaluate(bank.code, now).bankrupt) count++
        }
        return count
    }

    private fun liquidateLocked(bank: PrivateBankDto, now: Long): BankruptcyResult {
        bank.bankruptAt = now
        bank.star = 1
        bank.defaulterUntil = 0
        PrivateBankRepository.saveBank(bank)

        normalizeNegativeLedgers(bank.code, now)
        val liquidationDebt = PrivateBankDebtService.outstanding(PrivateBankDebtService.accrue(bank.code, now))

        var repaidFromPools = 0.0
        listOf(
            PrivateBankLedger.LIQUIDITY_DESC,
            PrivateBankLedger.RESERVE_DESC,
            PrivateBankLedger.INVENTORY_DESC,
        ).forEach { description ->
            val debt = PrivateBankDebtService.outstanding(PrivateBankDebtService.accrue(bank.code, now))
            if (debt <= EPSILON) return@forEach
            val balance = PrivateBankLedger.balance(bank.code, description).coerceAtLeast(0.0)
            val payment = balance.coerceAtMost(debt)
            if (payment <= EPSILON) return@forEach
            if (!PrivateBankLedger.debit(bank.code, description, payment)) {
                error("破产清算失败：${description} 扣减失败")
            }
            val repayment = runCatching { PrivateBankDebtService.repay(bank.code, payment, now) }.getOrElse {
                PrivateBankLedger.add(bank.code, description, payment)
                throw it
            }
            val applied = ShareUtils.rounding(repayment.paidInterest + repayment.paidPrincipal)
            if (payment - applied > EPSILON) PrivateBankLedger.add(bank.code, description, payment - applied)
            repaidFromPools = ShareUtils.rounding(repaidFromPools + applied)
        }

        val remainingDebt = PrivateBankDebtService.outstanding(PrivateBankDebtService.accrue(bank.code, now))
        if (remainingDebt > EPSILON) transferDebtToOwner(bank, remainingDebt, now)

        clearLedgers(bank.code)
        val depositsWrittenOff = writeOffDeposits(bank.code, now)
        val loansVoided = closeLoanBusiness(bank.code, now)
        closeBondBusiness(bank.code, now)
        clearDefaultBankSelections(bank.code)

        Log.warning(
            "银行破产: code=${bank.code}, debt=$liquidationDebt, pools=$repaidFromPools, " +
                "owner=${ShareUtils.rounding((liquidationDebt - repaidFromPools).coerceAtLeast(0.0))}, " +
                "deposits=$depositsWrittenOff, loans=$loansVoided"
        )
        return BankruptcyResult(
            bankrupt = true,
            debtAtTrigger = liquidationDebt,
            repaidFromPools = repaidFromPools,
            transferredToOwner = ShareUtils.rounding((liquidationDebt - repaidFromPools).coerceAtLeast(0.0)),
            depositsWrittenOff = depositsWrittenOff,
            loansVoided = loansVoided,
        )
    }

    private fun normalizeNegativeLedgers(bankCode: String, now: Long) {
        listOf(
            PrivateBankLedger.RESERVE_DESC,
            PrivateBankLedger.LIQUIDITY_DESC,
            PrivateBankLedger.INVENTORY_DESC,
        ).forEach { description ->
            val balance = PrivateBankLedger.balance(bankCode, description)
            if (balance >= -EPSILON) return@forEach
            val deficit = ShareUtils.rounding(-balance)
            if (!PrivateBankLedger.add(bankCode, description, deficit)) {
                error("破产清算失败：${description} 负余额归零失败")
            }
            runCatching { PrivateBankDebtService.addPrincipal(bankCode, deficit, now) }.getOrElse {
                PrivateBankLedger.add(bankCode, description, -deficit)
                throw it
            }
        }
    }

    private fun transferDebtToOwner(bank: PrivateBankDto, amount: Double, now: Long) {
        val accountId = UserCoreManager.getUserInfo(bank.ownerQq)?.id?.takeIf { it.isNotBlank() }
            ?: error("破产清算失败：无法解析行长账户 owner=${bank.ownerQq}")
        val account = EconomyService.account(accountId, null)
        val (walletShare, bankShare) = ownerDebtShares(amount)

        if (!EconomyUtil.plusMoneyToWalletForAccount(account, -walletShare)) {
            error("破产清算失败：行长钱包债务转移失败")
        }
        if (!EconomyUtil.plusMoneyToBankForAccount(account, -bankShare)) {
            EconomyUtil.plusMoneyToWalletForAccount(account, walletShare)
            error("破产清算失败：行长主银行债务转移失败")
        }
        runCatching { PrivateBankDebtService.repay(bank.code, amount, now) }.getOrElse {
            EconomyUtil.plusMoneyToWalletForAccount(account, walletShare)
            EconomyUtil.plusMoneyToBankForAccount(account, bankShare)
            throw it
        }
    }

    private fun clearLedgers(bankCode: String) {
        listOf(
            PrivateBankLedger.RESERVE_DESC,
            PrivateBankLedger.LIQUIDITY_DESC,
            PrivateBankLedger.INVENTORY_DESC,
        ).forEach { description ->
            val balance = PrivateBankLedger.balance(bankCode, description)
            if (kotlin.math.abs(balance) > EPSILON && !PrivateBankLedger.add(bankCode, description, -balance)) {
                error("破产清算失败：${description} 清零失败")
            }
        }
    }

    private fun writeOffDeposits(bankCode: String, now: Long): Double {
        var total = 0.0
        PrivateBankRepository.listDeposits(bankCode).forEach { deposit ->
            total = ShareUtils.rounding(total + deposit.principal.coerceAtLeast(0.0))
            deposit.principal = 0.0
            deposit.updatedAt = now
            PrivateBankRepository.saveDeposit(deposit)
        }
        return total
    }

    private fun closeLoanBusiness(bankCode: String, now: Long): Int {
        PrivateBankRepository.listLoanOffers(bankCode).forEach { offer ->
            offer.remaining = 0.0
            offer.enabled = false
            PrivateBankRepository.saveLoanOffer(offer)
        }
        var voided = 0
        PrivateBankRepository.listLoansByBank(bankCode).forEach { loan ->
            if (voidLoan(loan, now)) {
                PrivateBankRepository.saveLoan(loan)
                voided++
            }
        }
        return voided
    }

    private fun closeBondBusiness(bankCode: String, now: Long) {
        PrivateBankRepository.listBondHoldings(bankCode).forEach { holding ->
            holding.principal = 0.0
            holding.redeemedAt = now
            PrivateBankRepository.saveBondHolding(holding)
        }
        PrivateBankRepository.listFoxBondHoldings(bankCode).forEach { holding ->
            holding.principal = 0.0
            holding.redeemedAt = now
            PrivateBankRepository.saveFoxBondHolding(holding)
        }
        PrivateBankRepository.listFoxBonds()
            .filter { it.winnerBankCode == bankCode && it.status != "FINISHED" && it.status != "CANCELLED" }
            .forEach {
                it.status = "CANCELLED"
                PrivateBankRepository.saveFoxBond(it)
            }
    }

    private fun clearDefaultBankSelections(bankCode: String) {
        val userProxy = EntityProxyRegistry.get<UserInfoDto>("user") ?: return
        userProxy.findWhere { it.defaultPrivateBankCode == bankCode }.forEach {
            it.defaultPrivateBankCode = ""
            userProxy.save(it)
        }
    }
}
