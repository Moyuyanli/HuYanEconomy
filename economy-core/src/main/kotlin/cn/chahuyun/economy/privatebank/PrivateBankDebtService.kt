package cn.chahuyun.economy.privatebank

import cn.chahuyun.economy.data.repository.PrivateBankRepository
import cn.chahuyun.economy.model.privatebank.PrivateBankMainBankDebtDto
import cn.chahuyun.economy.utils.ShareUtils
import kotlin.math.floor

object PrivateBankDebtService {
    const val DAILY_RATE = 0.02
    const val DAY_MILLIS = 86_400_000L
    private const val EPSILON = 0.0001

    data class DebtRepayment(
        val paidInterest: Double,
        val paidPrincipal: Double,
        val remainingInput: Double,
        val remainingDebt: Double,
    )

    fun outstanding(debt: PrivateBankMainBankDebtDto?): Double =
        debt?.let { ShareUtils.rounding(it.principal + it.accruedInterest) } ?: 0.0

    fun hasOutstanding(bankCode: String): Boolean =
        outstanding(PrivateBankRepository.findMainBankDebt(bankCode)) > EPSILON

    fun accrue(bankCode: String, now: Long = System.currentTimeMillis()): PrivateBankMainBankDebtDto? =
        PrivateBankLocks.withBankLock(bankCode) {
            val debt = PrivateBankRepository.findMainBankDebt(bankCode) ?: return@withBankLock null
            if (applyAccrual(debt, now)) PrivateBankRepository.saveMainBankDebt(debt) else debt
        }

    fun addPrincipal(bankCode: String, amount: Double, now: Long = System.currentTimeMillis()): PrivateBankMainBankDebtDto {
        require(amount > 0) { "amount must be positive" }
        return PrivateBankLocks.withBankLock(bankCode) {
            val debt = PrivateBankRepository.findMainBankDebt(bankCode) ?: PrivateBankMainBankDebtDto(
                bankCode = bankCode,
                lastAccruedAt = now,
                createdAt = now,
            )
            applyAccrual(debt, now)
            if (debt.principal <= EPSILON) debt.lastAccruedAt = now
            debt.principal = ShareUtils.rounding(debt.principal + amount)
            debt.updatedAt = now
            debt.repaidAt = 0
            PrivateBankRepository.saveMainBankDebt(debt)
        }
    }

    fun repay(bankCode: String, amount: Double, now: Long = System.currentTimeMillis()): DebtRepayment {
        if (amount <= EPSILON) return DebtRepayment(0.0, 0.0, amount.coerceAtLeast(0.0), outstanding(PrivateBankRepository.findMainBankDebt(bankCode)))
        return PrivateBankLocks.withBankLock(bankCode) {
            val debt = PrivateBankRepository.findMainBankDebt(bankCode)
                ?: return@withBankLock DebtRepayment(0.0, 0.0, amount, 0.0)
            applyAccrual(debt, now)
            val repayment = applyRepayment(debt, amount, now)
            val saved = PrivateBankRepository.saveMainBankDebt(debt)
            repayment.copy(remainingDebt = outstanding(saved))
        }
    }

    fun accrueAll(now: Long = System.currentTimeMillis()): Int {
        var updated = 0
        PrivateBankRepository.listMainBankDebts().forEach { debt ->
            PrivateBankLocks.withBankLock(debt.bankCode) {
                val current = PrivateBankRepository.findMainBankDebt(debt.bankCode) ?: return@withBankLock
                if (applyAccrual(current, now)) {
                    PrivateBankRepository.saveMainBankDebt(current)
                    updated++
                }
            }
        }
        return updated
    }

    fun calculateInterest(principal: Double, elapsedDays: Long): Double =
        ShareUtils.rounding(principal.coerceAtLeast(0.0) * DAILY_RATE * elapsedDays.coerceAtLeast(0))

    fun applyRepayment(debt: PrivateBankMainBankDebtDto, amount: Double, now: Long): DebtRepayment {
        var remaining = ShareUtils.rounding(amount.coerceAtLeast(0.0))
        val paidInterest = remaining.coerceAtMost(debt.accruedInterest)
        debt.accruedInterest = ShareUtils.rounding(debt.accruedInterest - paidInterest)
        remaining = ShareUtils.rounding(remaining - paidInterest)

        val paidPrincipal = remaining.coerceAtMost(debt.principal)
        debt.principal = ShareUtils.rounding(debt.principal - paidPrincipal)
        remaining = ShareUtils.rounding(remaining - paidPrincipal)
        debt.updatedAt = now
        if (outstanding(debt) <= EPSILON) {
            debt.principal = 0.0
            debt.accruedInterest = 0.0
            debt.repaidAt = now
        }
        return DebtRepayment(paidInterest, paidPrincipal, remaining, outstanding(debt))
    }

    fun applyAccrual(debt: PrivateBankMainBankDebtDto, now: Long): Boolean {
        if (debt.lastAccruedAt == 0L) {
            debt.lastAccruedAt = now
            debt.updatedAt = now
            return true
        }
        if (debt.principal <= EPSILON || now <= debt.lastAccruedAt) return false
        val elapsedDays = floor((now - debt.lastAccruedAt).toDouble() / DAY_MILLIS).toLong()
        if (elapsedDays <= 0) return false
        debt.accruedInterest = ShareUtils.rounding(debt.accruedInterest + calculateInterest(debt.principal, elapsedDays))
        debt.lastAccruedAt += elapsedDays * DAY_MILLIS
        debt.updatedAt = now
        return true
    }
}
