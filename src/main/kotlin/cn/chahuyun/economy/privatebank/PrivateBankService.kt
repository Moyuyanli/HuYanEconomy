package cn.chahuyun.economy.privatebank

import cn.chahuyun.economy.entity.bank.BankInfo
import cn.chahuyun.economy.entity.privatebank.*
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.ShareUtils
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.contact.User
import java.util.*
import kotlin.math.ln

object PrivateBankService {

    const val CREATE_THRESHOLD = 10_000_000.0
    const val GUARANTEE_AMOUNT = 10_000_000.0

    private const val RESERVE_RATIO = 0.8

    private fun baseInterest(): Int {
        val bankInfo = HibernateFactory.selectOneById(BankInfo::class.java, 1)
        return bankInfo?.interest ?: 0
    }

    fun generateBankCode(ownerQq: Long): String {
        val suffix = RandomUtil.randomStringUpper(6)
        return "pb-$ownerQq-$suffix"
    }

    fun createBank(owner: User, name: String): Pair<Boolean, String> {
        val bankBalance = EconomyUtil.getMoneyByBank(owner)
        if (bankBalance < CREATE_THRESHOLD) {
            return false to "创建失败：主银行余额需达到 10,000,000（当前 ${ShareUtils.rounding(bankBalance)}）"
        }

        val code = generateBankCode(owner.id)
        val bank = PrivateBank(
            code = code,
            name = name,
            ownerQq = owner.id,
            depositorInterest = (baseInterest() - 2).coerceAtLeast(0)
        )

        // 锁定保证金：主银行(global) -> 自定义账本(custom) 保证金池
        val ok = EconomyUtil.turnUserGlobalBankToPluginBankForId(
            user = owner,
            toUserId = code,
            toDescription = PrivateBankLedger.GUARANTEE_DESC,
            quantity = GUARANTEE_AMOUNT
        )
        if (!ok) {
            return false to "创建失败：保证金锁定失败（请确认主银行余额足够且经济服务可用）"
        }

        PrivateBankRepository.saveBank(bank)
        return true to "创建成功：${bank.name}（code=${bank.code}）已锁定 10,000,000 保证金"
    }

    fun deposit(user: User, bankCode: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"

        val bank = PrivateBankRepository.findBankByCode(bankCode) ?: return false to "未找到该私人银行：$bankCode"
        if (bank.vipOnly) {
            val list = (bank.vipWhitelist ?: "").split(',').mapNotNull { it.trim().takeIf(String::isNotBlank) }
            if (list.isNotEmpty() && user.id.toString() !in list) {
                return false to "该私人银行仅对 VIP 开放"
            }
        }

        val wallet = EconomyUtil.getMoneyByUser(user)
        if (wallet - amount < 0) return false to "钱包余额不足（当前 ${ShareUtils.rounding(wallet)}）"

        val reservePart = ShareUtils.rounding(amount * RESERVE_RATIO)
        val liquidityPart = ShareUtils.rounding(amount - reservePart)

        // 1) reservePart：钱包(custom) -> 主银行(global) bankCode/pb-reserve
        if (!EconomyUtil.turnUserWalletToGlobalBankAccount(user, bank.code, PrivateBankLedger.RESERVE_DESC, reservePart)) {
            return false to "存入失败：准备金划转失败"
        }

        // 2) liquidityPart：钱包扣减 + 进入流动金池(custom)
        if (!EconomyUtil.minusMoneyToUser(user, liquidityPart)) {
            // 回滚准备金
            EconomyUtil.turnGlobalBankAccountToUserWallet(bank.code, PrivateBankLedger.RESERVE_DESC, user, reservePart)
            return false to "存入失败：钱包扣减失败"
        }
        if (!EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC, liquidityPart)) {
            // 回滚
            EconomyUtil.plusMoneyToUser(user, liquidityPart)
            EconomyUtil.turnGlobalBankAccountToUserWallet(bank.code, PrivateBankLedger.RESERVE_DESC, user, reservePart)
            return false to "存入失败：流动金池入账失败"
        }

        val deposit = PrivateBankRepository.findDeposit(bank.code, user.id) ?: PrivateBankDeposit(
            bankCode = bank.code,
            userQq = user.id,
            principal = 0.0
        )
        deposit.principal = ShareUtils.rounding(deposit.principal + amount)
        deposit.updatedAt = Date()
        PrivateBankRepository.saveDeposit(deposit)

        refreshRating(bank.code)

        return true to "存入成功：$amount（准备金 ${ShareUtils.rounding(reservePart)} / 流动金 ${ShareUtils.rounding(liquidityPart)}）"
    }

    fun withdraw(user: User, bankCode: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val bank = PrivateBankRepository.findBankByCode(bankCode) ?: return false to "未找到该私人银行：$bankCode"

        val deposit = PrivateBankRepository.findDeposit(bank.code, user.id)
            ?: return false to "你在该私人银行没有存款"

        if (deposit.principal - amount < 0) {
            return false to "取款失败：你的存款不足（当前 ${ShareUtils.rounding(deposit.principal)}）"
        }

        bank.withdrawRequests += 1
        PrivateBankRepository.saveBank(bank)

        val ok = forcePayToUserWallet(bank, amount, user)
        return if (ok) {
            deposit.principal = ShareUtils.rounding(deposit.principal - amount)
            deposit.updatedAt = Date()
            PrivateBankRepository.saveDeposit(deposit)
            refreshRating(bank.code)
            true to "取款成功：$amount"
        } else {
            bank.withdrawFailures += 1
            bank.defaulterUntil = DateUtil.offsetDay(Date(), 30)
            bank.depositorInterest = baseInterest() // 失信期间强制同步主银行利率
            PrivateBankRepository.saveBank(bank)
            refreshRating(bank.code)
            false to "取款失败：该私人银行资金链不足，已标记为失信银行（30天）"
        }
    }

    private fun forcePayToUserWallet(bank: PrivateBank, amount: Double, user: User): Boolean {
        var remaining = amount

        // 1) 流动金池(custom)
        val liquidity = EconomyUtil.getMoneyFromPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC)
        val takeLiquidity = liquidity.coerceAtMost(remaining)
        if (takeLiquidity > 0) {
            if (!EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC, -takeLiquidity)) return false
            if (!EconomyUtil.plusMoneyToUser(user, takeLiquidity)) {
                EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC, takeLiquidity)
                return false
            }
            remaining -= takeLiquidity
        }

        if (remaining <= 0) return true

        // 2) 准备金池(global)
        val reserve = EconomyUtil.getMoneyByBankFromId(bank.code, PrivateBankLedger.RESERVE_DESC)
        val takeReserve = reserve.coerceAtMost(remaining)
        if (takeReserve > 0) {
            if (!EconomyUtil.turnGlobalBankAccountToUserWallet(bank.code, PrivateBankLedger.RESERVE_DESC, user, takeReserve)) return false
            remaining -= takeReserve
        }

        if (remaining <= 0) return true

        // 3) 银行库存(custom)
        val inventory = EconomyUtil.getMoneyFromPluginBankForId(bank.code, PrivateBankLedger.INVENTORY_DESC)
        val takeInventory = inventory.coerceAtMost(remaining)
        if (takeInventory > 0) {
            if (!EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.INVENTORY_DESC, -takeInventory)) return false
            if (!EconomyUtil.plusMoneyToUser(user, takeInventory)) {
                EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.INVENTORY_DESC, takeInventory)
                return false
            }
            remaining -= takeInventory
        }

        if (remaining <= 0) return true

        // 4) 行长个人钱包(custom)
        val ownerUser = runCatching { user.bot.getFriend(bank.ownerQq) ?: user.bot.getStranger(bank.ownerQq) }.getOrNull()
        if (ownerUser != null) {
            val ownerWallet = EconomyUtil.getMoneyByUser(ownerUser)
            val takeOwner = ownerWallet.coerceAtMost(remaining)
            if (takeOwner > 0) {
                if (!EconomyUtil.minusMoneyToUser(ownerUser, takeOwner)) return false
                if (!EconomyUtil.plusMoneyToUser(user, takeOwner)) {
                    EconomyUtil.plusMoneyToUser(ownerUser, takeOwner)
                    return false
                }
                remaining -= takeOwner
            }
        }

        if (remaining <= 0) return true

        // 5) 国卷资产（强制提前折价赎回）
        remaining = forceLiquidateBonds(bank, remaining, user) ?: return false
        if (remaining <= 0) return true

        // 6) 风险保证金(custom)
        val guarantee = EconomyUtil.getMoneyFromPluginBankForId(bank.code, PrivateBankLedger.GUARANTEE_DESC)
        val takeGuarantee = guarantee.coerceAtMost(remaining)
        if (takeGuarantee > 0) {
            if (!EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.GUARANTEE_DESC, -takeGuarantee)) return false
            if (!EconomyUtil.plusMoneyToUser(user, takeGuarantee)) {
                EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.GUARANTEE_DESC, takeGuarantee)
                return false
            }
            remaining -= takeGuarantee
        }

        return remaining <= 0
    }

    /**
     * 强制提前赎回国卷（折价 10%），把资金直接支付给取款用户。
     * 返回剩余未覆盖金额；返回 null 表示处理异常。
     */
    private fun forceLiquidateBonds(bank: PrivateBank, remaining: Double, user: User): Double? {
        var left = remaining
        return try {
            val holdings = PrivateBankRepository.listBondHoldings(bank.code).filter { it.redeemedAt == null }
                .sortedBy { it.boughtAt.time }

            for (h in holdings) {
                if (left <= 0) break
                val redeemable = (h.principal * 0.9).coerceAtMost(left) // 折价
                if (redeemable <= 0) continue

                // 按比例缩减持仓
                val ratio = redeemable / (h.principal * 0.9)
                h.principal = ShareUtils.rounding(h.principal * (1 - ratio))
                if (h.principal <= 0.0001) {
                    h.redeemedAt = Date()
                }
                PrivateBankRepository.saveBondHolding(h)

                if (!EconomyUtil.plusMoneyToUser(user, redeemable)) {
                    return null
                }
                left -= redeemable
            }
            left
        } catch (e: Exception) {
            Log.error("私人银行:强制赎回国卷失败", e)
            null
        }
    }

    fun addReview(user: User, bankCode: String, rating: Int): Pair<Boolean, String> {
        if (rating !in 1..5) return false to "评分必须在 1-5"
        val bank = PrivateBankRepository.findBankByCode(bankCode) ?: return false to "未找到该私人银行：$bankCode"

        PrivateBankRepository.addReview(
            PrivateBankReview(
                bankCode = bank.code,
                userQq = user.id,
                rating = rating
            )
        )

        refreshRating(bank.code)
        return true to "评价成功：$rating 分"
    }

    fun refreshRating(bankCode: String) {
        val bank = PrivateBankRepository.findBankByCode(bankCode) ?: return
        val deposits = PrivateBankRepository.listDeposits(bank.code)
        val totalDeposit = deposits.sumOf { it.principal }

        val successRate = if (bank.withdrawRequests <= 0) 1.0 else
            ((bank.withdrawRequests - bank.withdrawFailures).toDouble() / bank.withdrawRequests.toDouble()).coerceIn(0.0, 1.0)

        val reviews = PrivateBankRepository.listReviews(bank.code)
        val avgReview = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()
        bank.avgReview = avgReview

        // 简单评分模型：存款规模(对数) + 取款成功率 + 评价
        val depositScore = if (totalDeposit <= 0) 0.0 else (ln(totalDeposit + 1) / ln(10.0) / 7.0).coerceIn(0.0, 1.0)
        val reviewScore = (avgReview / 5.0).coerceIn(0.0, 1.0)

        val score = 0.45 * depositScore + 0.35 * successRate + 0.20 * reviewScore
        bank.star = (1 + (score * 4.0)).toInt().coerceIn(1, 5)

        PrivateBankRepository.saveBank(bank)
    }

    fun ensureWeeklyBondIssue(): PrivateBankGovBondIssue {
        val now = Date()
        val weekKey = DateUtil.format(now, "yyyy-'W'ww")
        val existing = PrivateBankRepository.findBondIssueByWeek(weekKey)
        if (existing != null) return existing

        val issue = PrivateBankGovBondIssue(
            weekKey = weekKey,
            rateMultiplier = RandomUtil.randomDouble(2.0, 3.01),
            lockDays = RandomUtil.randomInt(3, 8),
            totalLimit = 2_000_000.0,
            remaining = 2_000_000.0,
            createdAt = now
        )
        return PrivateBankRepository.saveBondIssue(issue)
    }

    fun buyBond(owner: User, bankCode: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val bank = PrivateBankRepository.findBankByCode(bankCode) ?: return false to "未找到该私人银行：$bankCode"
        if (bank.ownerQq != owner.id) return false to "只有行长可以购买国卷"

        val issue = ensureWeeklyBondIssue()
        if (issue.remaining < amount) return false to "本周国卷剩余额度不足（剩余 ${ShareUtils.rounding(issue.remaining)}）"

        val liquidity = EconomyUtil.getMoneyFromPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC)
        if (liquidity < amount) return false to "流动金池余额不足（当前 ${ShareUtils.rounding(liquidity)}）"

        if (!EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC, -amount)) {
            return false to "购买失败：扣款失败"
        }

        issue.remaining = ShareUtils.rounding(issue.remaining - amount)
        PrivateBankRepository.saveBondIssue(issue)

        PrivateBankRepository.saveBondHolding(
            PrivateBankGovBondHolding(
                bankCode = bank.code,
                issueId = issue.id,
                principal = amount,
                rateMultiplier = issue.rateMultiplier,
                lockDays = issue.lockDays,
                boughtAt = Date()
            )
        )

        return true to "购买成功：国卷 $amount（锁仓 ${issue.lockDays} 天，倍数 ${String.format("%.2f", issue.rateMultiplier)}x）"
    }

    fun redeemBond(owner: User, holdingId: Int): Pair<Boolean, String> {
        val holding = PrivateBankRepository.findBondHolding(holdingId) ?: return false to "未找到该持仓"
        val bank = PrivateBankRepository.findBankByCode(holding.bankCode) ?: return false to "未找到对应银行"
        if (bank.ownerQq != owner.id) return false to "只有行长可以赎回国卷"
        if (holding.redeemedAt != null) return false to "该持仓已赎回"

        val days = DateUtil.between(holding.boughtAt, Date(), DateUnit.DAY)
        val base = baseInterest()

        val payout = if (days >= holding.lockDays) {
            // 到期：按主银行利率 * 倍数计算一次性收益（简化：按天计息）
            val rate = (base / 1000.0) * holding.rateMultiplier
            ShareUtils.rounding(holding.principal * (1 + rate * holding.lockDays))
        } else {
            // 提前：折价 10%
            ShareUtils.rounding(holding.principal * 0.9)
        }

        holding.redeemedAt = Date()
        PrivateBankRepository.saveBondHolding(holding)

        // 赎回资金进入流动金池
        EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC, payout)

        return true to "赎回成功：到账 ${ShareUtils.rounding(payout)}"
    }

    fun publishLoan(owner: User, bankCode: String, total: Double, interest: Int, termDays: Int, source: String): Pair<Boolean, String> {
        if (total <= 0) return false to "金额必须大于 0"
        if (termDays !in 1..30) return false to "期限仅支持 1-30 天"

        val bank = PrivateBankRepository.findBankByCode(bankCode) ?: return false to "未找到该私人银行：$bankCode"
        if (bank.ownerQq != owner.id) return false to "只有行长可以发布贷款"

        val src = source.uppercase(Locale.getDefault())
        if (src != "LIQUIDITY" && src != "OWNER") return false to "source 仅支持 LIQUIDITY/OWNER"

        // 先把资金冻结到库存账户，避免后续借款时资金不足
        val ok = if (src == "LIQUIDITY") {
            val liquidity = EconomyUtil.getMoneyFromPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC)
            if (liquidity < total) return false to "流动金池余额不足"
            EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC, -total) &&
                EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.INVENTORY_DESC, total)
        } else {
            val ownerWallet = EconomyUtil.getMoneyByUser(owner)
            if (ownerWallet < total) return false to "行长钱包余额不足"
            EconomyUtil.minusMoneyToUser(owner, total) &&
                EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.INVENTORY_DESC, total)
        }

        if (!ok) return false to "发布失败：资金冻结失败"

        val offer = PrivateBankLoanOffer(
            bankCode = bank.code,
            ownerQq = owner.id,
            source = src,
            total = total,
            remaining = total,
            interest = interest,
            termDays = termDays
        )
        PrivateBankRepository.saveLoanOffer(offer)
        return true to "发布成功：offerId=${offer.id} 可借 ${ShareUtils.rounding(offer.remaining)}"
    }

    fun borrow(user: User, offerId: Int, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val offer = PrivateBankRepository.findLoanOffer(offerId) ?: return false to "未找到该贷款标的"
        if (!offer.enabled) return false to "该贷款标的已关闭"
        if (offer.remaining < amount) return false to "剩余额度不足（剩余 ${ShareUtils.rounding(offer.remaining)}）"

        // 从库存账户放款到借款人钱包
        if (!EconomyUtil.plusMoneyToPluginBankForId(offer.bankCode, PrivateBankLedger.INVENTORY_DESC, -amount)) {
            return false to "放款失败：库存扣减失败"
        }
        if (!EconomyUtil.plusMoneyToUser(user, amount)) {
            EconomyUtil.plusMoneyToPluginBankForId(offer.bankCode, PrivateBankLedger.INVENTORY_DESC, amount)
            return false to "放款失败：钱包入账失败"
        }

        offer.remaining = ShareUtils.rounding(offer.remaining - amount)
        PrivateBankRepository.saveLoanOffer(offer)

        val dueAt = DateUtil.offsetDay(Date(), offer.termDays)
        val loan = PrivateBankLoan(
            offerId = offer.id,
            bankCode = offer.bankCode,
            lenderQq = offer.ownerQq,
            borrowerQq = user.id,
            principal = amount,
            interest = offer.interest,
            termDays = offer.termDays,
            createdAt = Date(),
            dueAt = dueAt
        )
        PrivateBankRepository.saveLoan(loan)

        return true to "借款成功：loanId=${loan.id} 到期日 ${DateUtil.formatDateTime(dueAt)}"
    }

    fun repay(user: User, loanId: Int): Pair<Boolean, String> {
        val loan = PrivateBankRepository.findLoan(loanId) ?: return false to "未找到借款单"
        if (loan.borrowerQq != user.id) return false to "只能偿还自己的借款"
        if (loan.repaidAt != null) return false to "该借款已结清"

        val days = DateUtil.between(loan.createdAt, Date(), DateUnit.DAY).coerceAtLeast(1)
        val rate = loan.interest / 1000.0
        val repayAmount = ShareUtils.rounding(loan.principal * (1 + rate * days))

        val wallet = EconomyUtil.getMoneyByUser(user)
        if (wallet < repayAmount) return false to "钱包余额不足（需 ${ShareUtils.rounding(repayAmount)}）"

        if (!EconomyUtil.minusMoneyToUser(user, repayAmount)) return false to "扣款失败"
        // 还款进入流动金池
        EconomyUtil.plusMoneyToPluginBankForId(loan.bankCode, PrivateBankLedger.LIQUIDITY_DESC, repayAmount)

        loan.repaidAt = Date()
        PrivateBankRepository.saveLoan(loan)

        return true to "还款成功：${ShareUtils.rounding(repayAmount)}"
    }
}
