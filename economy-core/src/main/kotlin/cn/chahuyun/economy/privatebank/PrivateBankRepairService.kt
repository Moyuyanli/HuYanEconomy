package cn.chahuyun.economy.privatebank

import cn.chahuyun.economy.data.repository.PrivateBankRepository
import cn.chahuyun.economy.model.privatebank.PrivateBankLoanOfferDto
import cn.chahuyun.economy.utils.MoneyFormatUtil
import cn.chahuyun.economy.utils.ShareUtils

object PrivateBankRepairService {
    private const val EPSILON = 0.0001

    fun audit(codeRaw: String): Pair<Boolean, String> {
        val code = codeRaw.trim()
        val bank = PrivateBankRepository.findBankByCode(code) ?: return false to "审计失败：未找到银行 code=$code"
        return PrivateBankLocks.withBankLock(code) {
            val reserve = PrivateBankLedger.balance(code, PrivateBankLedger.RESERVE_DESC)
            val liquidity = PrivateBankLedger.balance(code, PrivateBankLedger.LIQUIDITY_DESC)
            val inventory = PrivateBankLedger.balance(code, PrivateBankLedger.INVENTORY_DESC)
            val offers = activeOffers(code)
            val offerRemaining = ShareUtils.rounding(offers.sumOf { it.remaining })
            val debt = PrivateBankDebtService.accrue(code)
            val anomalies = buildList {
                if (inventory < -EPSILON) add("放贷库存为负 ${MoneyFormatUtil.format(inventory)}")
                if (kotlin.math.abs(inventory - offerRemaining) > EPSILON) {
                    add("库存与标的剩余额度相差 ${MoneyFormatUtil.format(inventory - offerRemaining)}")
                }
            }
            true to buildString {
                appendLine("私人银行审计：${bank.name}(code=$code)")
                appendLine("状态：${if (bank.isBankrupt()) "已破产" else "营业中"}")
                appendLine("准备资金池：${MoneyFormatUtil.format(reserve)}")
                appendLine("流动资金池：${MoneyFormatUtil.format(liquidity)}")
                appendLine("放贷库存：${MoneyFormatUtil.format(inventory)}")
                appendLine("启用标的剩余额度：${MoneyFormatUtil.format(offerRemaining)}（${offers.size} 笔）")
                offers.sortedWith(offerPriority()).forEach {
                    appendLine("- offerId=${it.id} 利率=${PrivateBankService.loanInterestPercentText(it.interest)} remaining=${MoneyFormatUtil.format(it.remaining)}")
                }
                appendLine("主银行债务：本金 ${MoneyFormatUtil.format(debt?.principal ?: 0.0)} / 利息 ${MoneyFormatUtil.format(debt?.accruedInterest ?: 0.0)}")
                append("异常：").append(if (anomalies.isEmpty()) "无" else anomalies.joinToString("；"))
            }
        }
    }

    fun correctDeposit(
        codeRaw: String,
        userQq: Long,
        correctPrincipal: Double,
        confirmation: String,
    ): Pair<Boolean, String> {
        if (!confirmation.equals("confirm", ignoreCase = true)) return false to "未修改：末尾必须输入 confirm"
        if (correctPrincipal < 0) return false to "修复失败：正确本金不能小于 0"
        val code = codeRaw.trim()
        if (PrivateBankRepository.findBankByCode(code) == null) return false to "修复失败：未找到银行 code=$code"
        PrivateBankBankruptcyService.evaluate(code)
        if (PrivateBankRepository.findBankByCode(code)?.isBankrupt() == true) {
            return false to "修复失败：银行已经破产，账本已永久关闭"
        }
        return PrivateBankLocks.withBankLock(code) {
            val deposit = PrivateBankRepository.findDeposit(code, userQq)
                ?: return@withBankLock false to "修复失败：未找到该用户的私银存款"
            if (correctPrincipal > deposit.principal + EPSILON) {
                return@withBankLock false to "修复失败：该命令只允许下调本金（当前 ${MoneyFormatUtil.format(deposit.principal)}）"
            }
            val before = deposit.principal
            deposit.principal = ShareUtils.rounding(correctPrincipal)
            deposit.updatedAt = System.currentTimeMillis()
            PrivateBankRepository.saveDeposit(deposit)
            PrivateBankService.refreshRating(code)
            true to "存款本金已校正：bank=$code user=$userQq ${MoneyFormatUtil.format(before)} -> ${MoneyFormatUtil.format(deposit.principal)}"
        }
    }

    fun reconcile(codeRaw: String, confirmation: String): Pair<Boolean, String> {
        if (!confirmation.equals("confirm", ignoreCase = true)) return false to "未修改：末尾必须输入 confirm"
        val code = codeRaw.trim()
        val bank = PrivateBankRepository.findBankByCode(code) ?: return false to "修复失败：未找到银行 code=$code"
        PrivateBankBankruptcyService.evaluate(code)
        if (PrivateBankRepository.findBankByCode(code)?.isBankrupt() == true) {
            return false to "修复失败：银行已经破产，账本已永久关闭"
        }
        return PrivateBankLocks.withBankLock(code) {
            val changes = mutableListOf<String>()
            var inventory = PrivateBankLedger.balance(code, PrivateBankLedger.INVENTORY_DESC)

            if (inventory < -EPSILON) {
                var deficit = ShareUtils.rounding(-inventory)
                val liquidity = PrivateBankLedger.balance(code, PrivateBankLedger.LIQUIDITY_DESC).coerceAtLeast(0.0)
                val reversed = deficit.coerceAtMost(liquidity)
                if (reversed > EPSILON) {
                    if (!PrivateBankLedger.debit(code, PrivateBankLedger.LIQUIDITY_DESC, reversed)) {
                        return@withBankLock false to "修复失败：流动池反冲扣减失败"
                    }
                    if (!PrivateBankLedger.add(code, PrivateBankLedger.INVENTORY_DESC, reversed)) {
                        PrivateBankLedger.add(code, PrivateBankLedger.LIQUIDITY_DESC, reversed)
                        return@withBankLock false to "修复失败：负库存反冲失败，流动池已回滚"
                    }
                    changes += "从流动池反冲 ${MoneyFormatUtil.format(reversed)}"
                    deficit = ShareUtils.rounding(deficit - reversed)
                }

                if (deficit > EPSILON) {
                    if (!PrivateBankLedger.add(code, PrivateBankLedger.INVENTORY_DESC, deficit)) {
                        return@withBankLock false to "修复失败：负库存归零失败"
                    }
                    val debtAdded = runCatching { PrivateBankDebtService.addPrincipal(code, deficit) }.isSuccess
                    if (!debtAdded) {
                        PrivateBankLedger.add(code, PrivateBankLedger.INVENTORY_DESC, -deficit)
                        if (reversed > EPSILON) {
                            PrivateBankLedger.add(code, PrivateBankLedger.INVENTORY_DESC, -reversed)
                            PrivateBankLedger.add(code, PrivateBankLedger.LIQUIDITY_DESC, reversed)
                        }
                        return@withBankLock false to "修复失败：缺口债务登记失败，库存已回滚"
                    }
                    PrivateBankService.markDefaulter(bank)
                    changes += "无法反冲的 ${MoneyFormatUtil.format(deficit)} 已转主银行债务"
                }
                inventory = PrivateBankLedger.balance(code, PrivateBankLedger.INVENTORY_DESC)
            }

            val offers = activeOffers(code)
            val offerRemaining = ShareUtils.rounding(offers.sumOf { it.remaining })
            if (offerRemaining > inventory + EPSILON) {
                val shortage = ShareUtils.rounding(offerRemaining - inventory.coerceAtLeast(0.0))
                val plan = PrivateBankService.buildLoanOfferReclaimPlan(offers, shortage, shortage)
                val originals = offers.associateBy { it.id }.mapValues { it.value.copy() }
                try {
                    plan.forEach { reduction ->
                        val offer = offers.first { it.id == reduction.offerId }
                        offer.remaining = ShareUtils.rounding((offer.remaining - reduction.amount).coerceAtLeast(0.0))
                        PrivateBankRepository.saveLoanOffer(offer)
                    }
                } catch (e: Exception) {
                    originals.values.forEach { runCatching { PrivateBankRepository.saveLoanOffer(it) } }
                    return@withBankLock false to "修复失败：放贷标的同步失败，已尝试回滚"
                }
                changes += "削减无库存支持的标的额度 ${MoneyFormatUtil.format(plan.sumOf { it.amount })}"
            } else if (inventory > offerRemaining + EPSILON) {
                val excess = ShareUtils.rounding(inventory - offerRemaining)
                if (!PrivateBankLedger.debit(code, PrivateBankLedger.INVENTORY_DESC, excess)) {
                    return@withBankLock false to "修复失败：多余库存扣减失败"
                }
                if (!PrivateBankLedger.add(code, PrivateBankLedger.LIQUIDITY_DESC, excess)) {
                    PrivateBankLedger.add(code, PrivateBankLedger.INVENTORY_DESC, excess)
                    return@withBankLock false to "修复失败：多余库存回流失败，库存已回滚"
                }
                changes += "多余库存 ${MoneyFormatUtil.format(excess)} 已回流流动池"
            }

            val bankruptcy = PrivateBankBankruptcyService.evaluate(code)
            PrivateBankService.refreshRating(code)
            val bankruptcySuffix = if (bankruptcy.bankrupt) "；主银行债务超过 1G，银行已破产清算" else ""
            if (changes.isEmpty()) {
                true to "code=$code 账本与放贷标的一致，无需修复$bankruptcySuffix"
            } else {
                true to "code=$code 修复完成：${changes.joinToString("；")}$bankruptcySuffix"
            }
        }
    }

    private fun activeOffers(code: String): List<PrivateBankLoanOfferDto> =
        PrivateBankRepository.listLoanOffers(code).filter { it.enabled && it.remaining > EPSILON }

    private fun offerPriority(): Comparator<PrivateBankLoanOfferDto> =
        compareByDescending<PrivateBankLoanOfferDto> { it.interest }.thenBy { it.createdAt }.thenBy { it.id }
}
