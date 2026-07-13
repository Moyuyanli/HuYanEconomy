package cn.chahuyun.economy.privatebank

import cn.chahuyun.economy.data.repository.PrivateBankRepository
import cn.chahuyun.economy.manager.BankManager
import cn.chahuyun.economy.model.privatebank.*
import cn.chahuyun.economy.utils.*
import cn.hutool.core.date.DateUnit
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.User
import java.util.*
import kotlin.math.abs
import kotlin.math.ln

object PrivateBankService {

    /** 创建私人银行需要达到的主银行余额门槛 */
    const val CREATE_THRESHOLD = 100_000_000.0

    /** 创建时扣除并沉淀到私银资产的启动资金 */
    const val CREATE_STARTUP_AMOUNT = 100_000_000.0

    /** 存款/启动资金进入准备金池的比例，剩余部分进入流动金池。 */
    private const val RESERVE_RATIO = 0.8

    /** 新建私人银行默认星级。 */
    private const val DEFAULT_STAR = 3

    /** 默认贷款期限，单位：天。 */
    private const val LOAN_TERM_DAYS = 14

    private const val MIN_LOAN_INTEREST = 10
    private const val MAX_LOAN_INTEREST = 180

    private fun baseInterest(): Int {
        return BankManager.getBankInfo(1)?.interest ?: 0
    }

    /** 主银行基准利率，返回百分比数值，例如 0.8 表示 0.8%。 */
    fun baseInterestPercent(): Double = baseInterest() / 10.0

    private fun allowedInterestRange(base: Int): IntRange {
        // 允许在主银行利率上下浮动 0.3%，interest 内部单位是 0.1%，所以偏移 3。
        return (base - 3)..(base + 3)
    }

    private fun findBankByCodeOrName(key: String): PrivateBankDto? {
        val trimmed = key.trim()
        if (trimmed.isBlank()) return null
        return PrivateBankRepository.findBankByCode(trimmed)
            ?: PrivateBankRepository.listBanks().firstOrNull { it.name == trimmed }
    }

    fun getBank(key: String): PrivateBankDto? = findBankByCodeOrName(key)

    fun getDeposit(bankCode: String, userQq: Long): PrivateBankDepositDto? =
        PrivateBankRepository.findDeposit(bankCode, userQq)

    fun borrowFromBank(user: User, bankKey: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val bank = findBankByCodeOrName(bankKey) ?: return false to "未找到该银行：$bankKey"
        val offer = PrivateBankRepository.listLoanOffers(bank.code)
            .filter { it.enabled && it.remaining >= amount }
            .minByOrNull { it.interest }
            ?: return false to "该银行暂无可用贷款额度"
        return borrow(user, offer.id, amount)
    }

    fun canUserDepositToBank(userQq: Long, bank: PrivateBankDto): Boolean =
        bank.ownerQq != userQq

    fun isLoanInterestAllowed(interest: Int): Boolean =
        interest in MIN_LOAN_INTEREST..MAX_LOAN_INTEREST

    fun buildLoanOfferCancelMoves(remaining: Double): List<LoanFundMove> {
        val refund = ShareUtils.rounding(remaining.coerceAtLeast(0.0))
        if (refund <= 0.0001) return emptyList()
        return listOf(
            LoanFundMove(PrivateBankLedger.INVENTORY_DESC, -refund),
            LoanFundMove(PrivateBankLedger.LIQUIDITY_DESC, refund)
        )
    }

    private fun resolveUser(qq: Long): User? {
        val bot = Bot.instances.firstOrNull() ?: return null
        bot.getFriend(qq)?.let { return it }
        bot.getStranger(qq)?.let { return it }
        val info = cn.chahuyun.economy.manager.UserCoreManager.getUserInfo(qq)
        val groupId = info?.registerGroup ?: 0
        if (groupId != 0L) {
            bot.getGroup(groupId)?.get(qq)?.let { return it }
        }
        return null
    }

    fun generateBankCode(ownerQq: Long): String {
        val suffix = RandomUtil.randomStringUpper(6)
        return "pb-$ownerQq-$suffix"
    }

    private fun validateBankCode(code: String): Boolean {
        val c = code.trim()
        if (c.length !in 3..48) return false
        return c.all { it.isLetterOrDigit() || it == '-' || it == '_' }
    }

    fun createBank(owner: User, codeInput: String?, name: String): Pair<Boolean, String> {
        val existingOwnerBank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == owner.id }
        if (existingOwnerBank != null) {
            return false to "创建失败：你已拥有自己的银行(code=${existingOwnerBank.code})"
        }

        val bankBalance = EconomyUtil.getMoneyByBank(owner)
        if (bankBalance < CREATE_THRESHOLD) {
            return false to "创建失败：主银行余额需达到 100,000,000（当前 ${MoneyFormatUtil.format(bankBalance)}）"
        }

        val code = (codeInput ?: "").trim().ifBlank { generateBankCode(owner.id) }
        if (!validateBankCode(code)) {
            return false to "创建失败：code 只能包含字母、数字、-、_，长度 3-48"
        }
        if (PrivateBankRepository.findBankByCode(code) != null) {
            return false to "创建失败：code 已存在：$code"
        }

        val bank = PrivateBankDto(
            code = code,
            name = name,
            ownerQq = owner.id,
            depositorInterest = baseInterest(),
            star = DEFAULT_STAR
        )

        val reservePart = ShareUtils.rounding(CREATE_STARTUP_AMOUNT * RESERVE_RATIO)
        val liquidityPart = ShareUtils.rounding(CREATE_STARTUP_AMOUNT - reservePart)

        // 1) 80% 进入准备金池，存放在全局银行账户中。
        if (!EconomyUtil.turnUserGlobalBankToGlobalBankAccount(owner, bank.code, PrivateBankLedger.RESERVE_DESC, reservePart)) {
            return false to "创建失败：启动资金划转准备金失败"
        }

        // 2) 20% 进入流动金池，存放在插件自定义账户中。
        if (!EconomyUtil.turnUserGlobalBankToPluginBankForId(owner, bank.code, PrivateBankLedger.LIQUIDITY_DESC, liquidityPart)) {
            // 回滚准备金划转
            EconomyUtil.plusMoneyToBankFromId(bank.code, PrivateBankLedger.RESERVE_DESC, -reservePart)
            EconomyUtil.plusMoneyToBank(owner, reservePart)
            return false to "创建失败：启动资金划转流动金失败"
        }

        PrivateBankRepository.saveBank(bank)
        return true to "创建成功：${bank.name}(code=${bank.code}) 启动资金 100,000,000 已入账（准备金 ${MoneyFormatUtil.format(reservePart)} / 流动金 ${MoneyFormatUtil.format(liquidityPart)}）"
    }

    fun deposit(user: User, bankCode: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"

        val bank = findBankByCodeOrName(bankCode) ?: return false to "未找到该银行：$bankCode"
        if (!canUserDepositToBank(user.id, bank)) {
            return false to "行长不能向自己的银行存款"
        }
        if (bank.vipOnly) {
            val list = bank.vipWhitelist.split(',').mapNotNull { it.trim().takeIf(String::isNotBlank) }
            if (list.isNotEmpty() && user.id.toString() !in list) {
                return false to "该银行仅对 VIP 开放"
            }
        }

        val wallet = EconomyUtil.getMoneyByUser(user)
        if (wallet - amount < 0) return false to "钱包余额不足（当前 ${MoneyFormatUtil.format(wallet)}）"

        val reservePart = ShareUtils.rounding(amount * RESERVE_RATIO)
        val liquidityPart = ShareUtils.rounding(amount - reservePart)

        // 1) 准备金从用户钱包进入全局银行准备金池。
        if (!EconomyUtil.turnUserWalletToGlobalBankAccount(user, bank.code, PrivateBankLedger.RESERVE_DESC, reservePart)) {
            return false to "存入失败：准备金划转失败"
        }

        // 2) 流动金从用户钱包扣除后进入插件流动金池。
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

        val deposit = PrivateBankRepository.findDeposit(bank.code, user.id) ?: PrivateBankDepositDto(
            bankCode = bank.code,
            userQq = user.id,
            principal = 0.0
        )
        deposit.principal = ShareUtils.rounding(deposit.principal + amount)
        deposit.updatedAt = System.currentTimeMillis()
        PrivateBankRepository.saveDeposit(deposit)

        refreshRating(bank.code)

        return true to "存入成功：${MoneyFormatUtil.format(amount)} 已存入 ${bank.name}(code=${bank.code})"
    }

    fun withdraw(user: User, bankCode: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val bank = findBankByCodeOrName(bankCode) ?: return false to "未找到该银行：$bankCode"

        val deposit = PrivateBankRepository.findDeposit(bank.code, user.id)
            ?: return false to "你在该银行没有存款"

        if (deposit.principal - amount < 0) {
            return false to "取款失败：你的存款不足（当前 ${MoneyFormatUtil.format(deposit.principal)}）"
        }

        bank.withdrawRequests += 1
        PrivateBankRepository.saveBank(bank)

        val ok = forcePayToUserWallet(bank, amount, user)
        return if (ok) {
            deposit.principal = ShareUtils.rounding(deposit.principal - amount)
            deposit.updatedAt = System.currentTimeMillis()
            PrivateBankRepository.saveDeposit(deposit)
            refreshRating(bank.code)
            true to "取款成功：${MoneyFormatUtil.format(amount)}"
        } else {
            bank.withdrawFailures += 1
            bank.defaulterUntil = DateUtil.offsetDay(Date(), 30).time
            bank.depositorInterest = baseInterest() // 失信后利率回落到主银行基准
            PrivateBankRepository.saveBank(bank)
            refreshRating(bank.code)
            false to "取款失败：银行资金不足，已进入 30 天失信期"
        }
    }

    private fun forcePayToUserWallet(bank: PrivateBankDto, amount: Double, user: User): Boolean {
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

        // 6) 风险保证金池(custom)
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
     * 强制提前折价赎回国卷资产，按 90% 本金计算可用金额。
     * 赎回过程中失败时返回 null，表示本次强制兑付失败。
     */
    private fun forceLiquidateBonds(bank: PrivateBankDto, remaining: Double, user: User): Double? {
        var left = remaining
        return try {
            val holdings = PrivateBankRepository.listBondHoldings(bank.code).filter { it.redeemedAt == 0L }
                .sortedBy { it.boughtAt }

            for (h in holdings) {
                if (left <= 0) break
                val redeemable = (h.principal * 0.9).coerceAtMost(left) // 折价
                if (redeemable <= 0) continue

                // 按本次赎回金额等比例扣减持仓本金。
                val ratio = redeemable / (h.principal * 0.9)
                h.principal = ShareUtils.rounding(h.principal * (1 - ratio))
                if (h.principal <= 0.0001) {
                    h.redeemedAt = System.currentTimeMillis()
                }
                PrivateBankRepository.saveBondHolding(h)

                if (!EconomyUtil.plusMoneyToUser(user, redeemable)) {
                    return null
                }
                left -= redeemable
            }
            left
        } catch (e: Exception) {
            Log.error("银行:强制赎回国卷失败", e)
            null
        }
    }

    fun addReview(user: User, bankCode: String, rating: Int): Pair<Boolean, String> {
        return addReview(user, bankCode, rating, null)
    }

    fun addReview(user: User, bankCode: String, rating: Int, content: String?): Pair<Boolean, String> {
        if (rating !in 1..5) return false to "评分必须为 1-5"
        val bank = findBankByCodeOrName(bankCode) ?: return false to "未找到该银行：$bankCode"

        val deposit = PrivateBankRepository.findDeposit(bank.code, user.id)
            ?: return false to "你需要先在该银行存款后才能评分"

        val days = DateUtil.between(Date(deposit.createdAt), Date(), DateUnit.DAY)
        if (days < 7) {
            return false to "存款满 7 天后才能评分（当前 ${days} 天）"
        }

        val now = Date()
        val recentCount = PrivateBankRepository.listReviewsByUser(bank.code, user.id)
            .count { DateUtil.between(Date(it.createdAt), now, DateUnit.DAY) < 30 }
        if (recentCount >= 2) {
            return false to "评分失败：一个月内最多评分 2 次"
        }

        val clean = content?.trim()?.takeIf { it.isNotBlank() }?.take(500)
        PrivateBankRepository.addReview(
            PrivateBankReviewDto(
                bankCode = bank.code,
                userQq = user.id,
                rating = rating,
                content = clean ?: ""
            )
        )

        refreshRating(bank.code)
        return true to "评价成功：$rating 分"
    }

    fun setDepositorInterest(owner: User, bankCode: String, modeOrRate: String): Pair<Boolean, String> {
        val bank = findBankByCodeOrName(bankCode) ?: return false to "未找到该银行：$bankCode"
        if (bank.ownerQq != owner.id) return false to "只有行长可以设置利率"
        if (bank.isDefaulter()) return false to "失信期间禁止调整利率"

        val base = baseInterest()
        val allowed = allowedInterestRange(base)

        val nextInterest = when (val key = modeOrRate.trim().lowercase(Locale.getDefault())) {
            "max" -> allowed.last
            "min" -> allowed.first
            "now" -> base
            else -> {
                // 兼容 1.2 这种百分比写法，会转换为内部 12 的 interest 值。
                val v = key.toDoubleOrNull() ?: return false to "利率参数错误，请使用 rate/max/min/now"
                val asInterest = if (v < 10) {
                    // 视为百分比，例如 1.2 -> 12
                    (v * 10.0).toInt()
                } else {
                    v.toInt()
                }
                asInterest
            }
        }

        if (nextInterest !in allowed) {
            return false to "利率超出允许范围：${FormatUtil.fixed(allowed.first / 10.0, 1)}% - ${FormatUtil.fixed(allowed.last / 10.0, 1)}%"
        }

        bank.depositorInterest = nextInterest
        PrivateBankRepository.saveBank(bank)
        refreshRating(bank.code)
        return true to "利率已更新：${FormatUtil.fixed(bank.depositorInterest / 10.0, 1)}%"
    }

    private fun maxLoanOffersByStar(star: Int): Int = star.coerceIn(1, 5)

    private fun parseLoanInterest(ratePercent: Double): Int =
        (ratePercent * 10.0).toInt()

    fun publishLoanByPlan(owner: User, bankCode: String, total: Double, ratePercent: Double): Pair<Boolean, String> {
        val interest = parseLoanInterest(ratePercent)
        return publishLoan(owner, bankCode, total, interest, LOAN_TERM_DAYS, "LIQUIDITY")
    }

    fun updateLoanOfferInterest(owner: User, offerId: Int, ratePercent: Double): Pair<Boolean, String> {
        val offer = PrivateBankRepository.findLoanOffer(offerId) ?: return false to "未找到贷款额度：$offerId"
        val bank = findBankByCodeOrName(offer.bankCode) ?: return false to "未找到对应银行：${offer.bankCode}"
        if (bank.ownerQq != owner.id) return false to "只有行长可以修改贷款利息"
        if (!offer.enabled || offer.remaining <= 0.0001) return false to "该贷款额度已不可修改"

        val interest = parseLoanInterest(ratePercent)
        if (!isLoanInterestAllowed(interest)) {
            return false to "贷款利息必须在 1.0% - 18.0% 之间"
        }

        offer.interest = interest
        PrivateBankRepository.saveLoanOffer(offer)
        return true to "贷款利息已更新：offerId=${offer.id} 日利率=${FormatUtil.fixed(offer.interest / 10.0, 1)}%"
    }

    fun cancelLoanOffer(owner: User, offerId: Int): Pair<Boolean, String> {
        val offer = PrivateBankRepository.findLoanOffer(offerId) ?: return false to "未找到贷款额度：$offerId"
        val bank = findBankByCodeOrName(offer.bankCode) ?: return false to "未找到对应银行：${offer.bankCode}"
        if (bank.ownerQq != owner.id) return false to "只有行长可以撤回贷款额度"
        if (!offer.enabled && offer.remaining <= 0.0001) return false to "该贷款额度已关闭"

        val moves = buildLoanOfferCancelMoves(offer.remaining)
        val refund = moves.firstOrNull { it.description == PrivateBankLedger.LIQUIDITY_DESC }?.amount ?: 0.0
        if (moves.isNotEmpty()) {
            val debit = moves.first { it.description == PrivateBankLedger.INVENTORY_DESC }
            val credit = moves.first { it.description == PrivateBankLedger.LIQUIDITY_DESC }
            if (!EconomyUtil.plusMoneyToPluginBankForId(offer.bankCode, debit.description, debit.amount)) {
                return false to "撤贷失败：放贷库存扣减失败"
            }
            if (!EconomyUtil.plusMoneyToPluginBankForId(offer.bankCode, credit.description, credit.amount)) {
                EconomyUtil.plusMoneyToPluginBankForId(offer.bankCode, debit.description, -debit.amount)
                return false to "撤贷失败：流动金池回流失败"
            }
        }

        offer.remaining = 0.0
        offer.enabled = false
        PrivateBankRepository.saveLoanOffer(offer)
        return true to "撤贷成功：offerId=${offer.id}，已回流 ${MoneyFormatUtil.format(refund)} 到流动金池"
    }

    data class LoanFundMove(
        val description: String,
        val amount: Double
    )

    data class LoanFundFreezePlan(
        val source: String,
        val requiredBalanceError: String,
        val moves: List<LoanFundMove>
    )

    fun buildLoanFundFreezePlan(source: String, total: Double): LoanFundFreezePlan? {
        val src = source.uppercase(Locale.getDefault())
        return when (src) {
            "LIQUIDITY" -> LoanFundFreezePlan(
                source = src,
                requiredBalanceError = "流动金池余额不足",
                moves = listOf(
                    LoanFundMove(PrivateBankLedger.LIQUIDITY_DESC, -total),
                    LoanFundMove(PrivateBankLedger.INVENTORY_DESC, total)
                )
            )
            "OWNER" -> LoanFundFreezePlan(
                source = src,
                requiredBalanceError = "行长钱包余额不足",
                moves = listOf(
                    LoanFundMove(PrivateBankLedger.INVENTORY_DESC, total)
                )
            )
            else -> null
        }
    }

    private fun freezeLoanFundsFromLiquidity(bankCode: String, plan: LoanFundFreezePlan): Boolean {
        val debit = plan.moves.firstOrNull { it.description == PrivateBankLedger.LIQUIDITY_DESC && it.amount < 0 }
            ?: return false
        val credit = plan.moves.firstOrNull { it.description == PrivateBankLedger.INVENTORY_DESC && it.amount > 0 }
            ?: return false

        val beforeLiquidity = EconomyUtil.getMoneyFromPluginBankForId(bankCode, debit.description)
        if (!EconomyUtil.plusMoneyToPluginBankForId(bankCode, debit.description, debit.amount)) {
            return false
        }
        val afterDebitLiquidity = EconomyUtil.getMoneyFromPluginBankForId(bankCode, debit.description)
        val expectedLiquidity = ShareUtils.rounding(beforeLiquidity + debit.amount)
        if (abs(afterDebitLiquidity - expectedLiquidity) > 0.1) {
            if (afterDebitLiquidity < beforeLiquidity - 0.1) {
                EconomyUtil.plusMoneyToPluginBankForId(bankCode, debit.description, -debit.amount)
            }
            return false
        }

        if (!EconomyUtil.plusMoneyToPluginBankForId(bankCode, credit.description, credit.amount)) {
            EconomyUtil.plusMoneyToPluginBankForId(bankCode, debit.description, -debit.amount)
            return false
        }
        return true
    }

    private fun freezeLoanFundsFromOwnerWallet(owner: User, bankCode: String, total: Double, plan: LoanFundFreezePlan): Boolean {
        val credit = plan.moves.firstOrNull { it.description == PrivateBankLedger.INVENTORY_DESC && it.amount > 0 }
            ?: return false

        if (!EconomyUtil.minusMoneyToUser(owner, total)) {
            return false
        }
        if (!EconomyUtil.plusMoneyToPluginBankForId(bankCode, credit.description, credit.amount)) {
            EconomyUtil.plusMoneyToUser(owner, total)
            return false
        }
        return true
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

        // 评分由存款规模、取款成功率、用户评价共同决定。
        val depositScore = if (totalDeposit <= 0) 0.0 else (ln(totalDeposit + 1) / ln(10.0) / 7.0).coerceIn(0.0, 1.0)
        val reviewScore = (avgReview / 5.0).coerceIn(0.0, 1.0)

        val score = 0.45 * depositScore + 0.35 * successRate + 0.20 * reviewScore
        bank.star = (1 + (score * 4.0)).toInt().coerceIn(1, 5)

        PrivateBankRepository.saveBank(bank)
    }

    fun ensureWeeklyBondIssue(): PrivateBankGovBondIssueDto {
        val now = Date()
        val weekKey = DateUtil.format(now, "yyyy-'W'ww")
        val existing = PrivateBankRepository.findBondIssueByWeek(weekKey)
        if (existing != null) return existing

        val issue = PrivateBankGovBondIssueDto(
            weekKey = weekKey,
            rateMultiplier = RandomUtil.randomDouble(2.0, 3.01),
            lockDays = RandomUtil.randomInt(3, 8),
            totalLimit = 2_000_000.0,
            remaining = 2_000_000.0,
            createdAt = now.time
        )
        return PrivateBankRepository.saveBondIssue(issue)
    }

    fun buyBond(owner: User, bankCode: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val bank = PrivateBankRepository.findBankByCode(bankCode) ?: return false to "未找到该银行：$bankCode"
        if (bank.ownerQq != owner.id) return false to "只有行长可以购买国卷"

        val issue = ensureWeeklyBondIssue()
        if (issue.remaining < amount) return false to "本周国债剩余额度不足（剩余 ${MoneyFormatUtil.format(issue.remaining)}）"

        val liquidity = EconomyUtil.getMoneyFromPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC)
        if (liquidity < amount) return false to "流动金池余额不足（当前 ${MoneyFormatUtil.format(liquidity)}）"

        if (!EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC, -amount)) {
            return false to "购买失败：扣款失败"
        }

        issue.remaining = ShareUtils.rounding(issue.remaining - amount)
        PrivateBankRepository.saveBondIssue(issue)

        PrivateBankRepository.saveBondHolding(
            PrivateBankGovBondHoldingDto(
                bankCode = bank.code,
                issueId = issue.id,
                principal = amount,
                rateMultiplier = issue.rateMultiplier,
                lockDays = issue.lockDays,
                boughtAt = System.currentTimeMillis()
            )
        )

        return true to "购买成功：国债 ${MoneyFormatUtil.format(amount)}（锁仓 ${issue.lockDays} 天，倍数 ${FormatUtil.fixed(issue.rateMultiplier, 2)}x）"
    }

    fun isBondMatured(holding: PrivateBankGovBondHoldingDto, now: Date = Date()): Boolean {
        val days = DateUtil.between(Date(holding.boughtAt), now, DateUnit.DAY)
        return days >= holding.lockDays
    }

    fun redeemBond(owner: User, holdingId: Int): Pair<Boolean, String> {
        val holding = PrivateBankRepository.findBondHolding(holdingId) ?: return false to "未找到该持仓"
        val bank = PrivateBankRepository.findBankByCode(holding.bankCode) ?: return false to "未找到对应银行"
        if (bank.ownerQq != owner.id) return false to "只有行长可以赎回国卷"
        if (holding.redeemedAt != 0L) return false to "该持仓已赎回"

        val base = baseInterest()

        val payout = if (isBondMatured(holding)) {
            // 到期赎回：本金 * 锁仓期收益。
            val rate = (base / 1000.0) * holding.rateMultiplier
            ShareUtils.rounding(holding.principal * (1 + rate * holding.lockDays))
        } else {
            // 未到期提前赎回扣 10%。
            ShareUtils.rounding(holding.principal * 0.9)
        }

        holding.redeemedAt = System.currentTimeMillis()
        PrivateBankRepository.saveBondHolding(holding)

        // 赎回资金进入流动金池
        EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC, payout)

        return true to "赎回成功：${MoneyFormatUtil.format(payout)}"
    }

    fun publishLoan(owner: User, bankCode: String, total: Double, interest: Int, termDays: Int, source: String): Pair<Boolean, String> {
        if (total <= 0) return false to "金额必须大于 0"
        if (termDays !in 1..30) return false to "期限仅支持 1-30 天"

        val bank = findBankByCodeOrName(bankCode) ?: return false to "未找到该银行：$bankCode"
        if (bank.ownerQq != owner.id) return false to "只有行长可以发布贷款"

        val activeOffers = PrivateBankRepository.listLoanOffers(bank.code)
            .count { it.enabled && it.remaining > 0.0001 }
        if (activeOffers >= maxLoanOffersByStar(bank.star)) {
            return false to "发布失败：当前银行星级最多允许 ${maxLoanOffersByStar(bank.star)} 笔贷款额度"
        }

        if (!isLoanInterestAllowed(interest)) {
            return false to "发布失败：贷款利息必须在 1.0% - 18.0% 之间"
        }

        val freezePlan = buildLoanFundFreezePlan(source, total)
            ?: return false to "source 仅支持 LIQUIDITY/OWNER"

        // 发布贷款前先冻结对应资金到放贷库存池。
        val ok = if (freezePlan.source == "LIQUIDITY") {
            val liquidity = EconomyUtil.getMoneyFromPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC)
            if (liquidity < total) return false to freezePlan.requiredBalanceError
            freezeLoanFundsFromLiquidity(bank.code, freezePlan)
        } else {
            val ownerWallet = EconomyUtil.getMoneyByUser(owner)
            if (ownerWallet < total) return false to freezePlan.requiredBalanceError
            freezeLoanFundsFromOwnerWallet(owner, bank.code, total, freezePlan)
        }

        if (!ok) return false to "发布失败：资金冻结失败"

        val offer = PrivateBankLoanOfferDto(
            bankCode = bank.code,
            ownerQq = owner.id,
            source = freezePlan.source,
            total = total,
            remaining = total,
            interest = interest,
            termDays = termDays,
            createdAt = System.currentTimeMillis()
        )
        val savedOffer = PrivateBankRepository.saveLoanOffer(offer)
        return true to "放贷已发布：offerId=${savedOffer.id} 额度=${MoneyFormatUtil.format(savedOffer.remaining)}"
    }

    fun borrow(user: User, offerId: Int, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val offer = PrivateBankRepository.findLoanOffer(offerId) ?: return false to "未找到贷款额度"
        if (!offer.enabled) return false to "该贷款额度已关闭"
        if (offer.remaining < amount) return false to "剩余额度不足（剩余 ${MoneyFormatUtil.format(offer.remaining)}）"

        // 从放贷库存池扣除放款金额。
        if (!EconomyUtil.plusMoneyToPluginBankForId(offer.bankCode, PrivateBankLedger.INVENTORY_DESC, -amount)) {
            return false to "放款失败：放贷库存扣减失败"
        }

        // 借款会进入用户在该银行的存款本金，实际资金进入银行流动金池。
        if (!EconomyUtil.plusMoneyToPluginBankForId(offer.bankCode, PrivateBankLedger.LIQUIDITY_DESC, amount)) {
            EconomyUtil.plusMoneyToPluginBankForId(offer.bankCode, PrivateBankLedger.INVENTORY_DESC, amount)
            return false to "放款失败：流动金池入账失败"
        }

        val dep = PrivateBankRepository.findDeposit(offer.bankCode, user.id)
            ?: PrivateBankDepositDto(bankCode = offer.bankCode, userQq = user.id, principal = 0.0)
        dep.principal = ShareUtils.rounding(dep.principal + amount)
        dep.updatedAt = System.currentTimeMillis()
        PrivateBankRepository.saveDeposit(dep)

        offer.remaining = ShareUtils.rounding(offer.remaining - amount)
        PrivateBankRepository.saveLoanOffer(offer)

        val dueAt = DateUtil.offsetDay(Date(), offer.termDays)
        val dueTotal = ShareUtils.rounding(amount * (1 + (offer.interest / 1000.0) * offer.termDays))
        val loan = PrivateBankLoanDto(
            offerId = offer.id,
            bankCode = offer.bankCode,
            lenderQq = offer.ownerQq,
            borrowerQq = user.id,
            principal = amount,
            dueTotal = dueTotal,
            repaidAmount = 0.0,
            interest = offer.interest,
            termDays = offer.termDays,
            createdAt = System.currentTimeMillis(),
            dueAt = dueAt.time
        )
        PrivateBankRepository.saveLoan(loan)
        val interestAmount = ShareUtils.rounding(dueTotal - amount)

        return true to "借款成功: 本金=${MoneyFormatUtil.format(amount)}, 应还=${MoneyFormatUtil.format(dueTotal)}, 利息=${MoneyFormatUtil.format(interestAmount)}, 最迟还款=${DateUtil.formatDateTime(dueAt)}"
    }

    fun repay(user: User, loanId: Int): Pair<Boolean, String> {
        val loan = PrivateBankRepository.findLoan(loanId) ?: return false to "未找到借款单"
        if (loan.borrowerQq != user.id) return false to "这不是你的借款单"
        if (loan.repaidAt != 0L) return false to "该借款已结清"
        val outstanding = ShareUtils.rounding((loan.dueTotal - loan.repaidAmount).coerceAtLeast(0.0))
        return repayByAmount(user, loanId, outstanding)
    }

    fun repayByAmount(user: User, loanId: Int, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val loan = PrivateBankRepository.findLoan(loanId) ?: return false to "未找到借款单"
        if (loan.borrowerQq != user.id) return false to "这不是你的借款单"
        if (loan.repaidAt != 0L) return false to "该借款已结清"

        val outstanding = (loan.dueTotal - loan.repaidAmount).coerceAtLeast(0.0)
        if (outstanding <= 0.0001) {
            loan.repaidAt = System.currentTimeMillis()
            PrivateBankRepository.saveLoan(loan)
            return true to "该借款已结清"
        }

        val pay = amount.coerceAtMost(outstanding)

        // 还款优先从钱包扣，不足时再从主银行扣。
        var remaining = pay
        val wallet = EconomyUtil.getMoneyByUser(user)
        val takeWallet = wallet.coerceAtMost(remaining)
        if (takeWallet > 0) {
            if (!EconomyUtil.minusMoneyToUser(user, takeWallet)) return false to "扣款失败"
            remaining -= takeWallet
        }
        if (remaining > 0) {
            val bank = EconomyUtil.getMoneyByBank(user)
            val takeBank = bank.coerceAtMost(remaining)
            if (takeBank > 0) {
                if (!EconomyUtil.plusMoneyToBank(user, -takeBank)) {
                    // 回滚钱包扣款
                    if (takeWallet > 0) EconomyUtil.plusMoneyToUser(user, takeWallet)
                    return false to "扣款失败"
                }
                remaining -= takeBank
            }
        }

        val paid = ShareUtils.rounding(pay - remaining)
        if (paid <= 0) return false to "余额不足"

        EconomyUtil.plusMoneyToPluginBankForId(loan.bankCode, PrivateBankLedger.LIQUIDITY_DESC, paid)
        loan.repaidAmount = ShareUtils.rounding(loan.repaidAmount + paid)
        if (loan.repaidAmount + 0.0001 >= loan.dueTotal) {
            loan.repaidAt = System.currentTimeMillis()
        }
        PrivateBankRepository.saveLoan(loan)

        val left = ShareUtils.rounding((loan.dueTotal - loan.repaidAmount).coerceAtLeast(0.0))
        return true to "还款成功：${MoneyFormatUtil.format(paid)}，剩余 ${MoneyFormatUtil.format(left)}"
    }

    fun repayToBankByAmount(user: User, bankCode: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val bank = findBankByCodeOrName(bankCode) ?: return false to "未找到该银行：$bankCode"
        val loans = PrivateBankRepository.listLoansByBorrower(user.id)
            .filter { it.repaidAt == 0L && it.bankCode == bank.code }
            .sortedBy { it.createdAt }

        if (loans.isEmpty()) return false to "你在该银行没有未结清借款"

        var remaining = amount
        var paidTotal = 0.0
        for (loan in loans) {
            if (remaining <= 0.0001) break
            val outstanding = (loan.dueTotal - loan.repaidAmount).coerceAtLeast(0.0)
            if (outstanding <= 0.0001) continue
            val pay = remaining.coerceAtMost(outstanding)
            val (ok, _) = repayByAmount(user, loan.id, pay)
            if (!ok) break
            paidTotal += pay
            remaining -= pay
        }

        return true to "已尝试还款：${MoneyFormatUtil.format(paidTotal)}"
    }

    /**
     * 定时追缴逾期贷款。
     * 无法解析到用户对象时跳过，避免定时任务中断。
     */
    fun collectOverdueLoans(): Int {
        val now = Date()
        var processed = 0
        val loans = PrivateBankRepository.listUnrepaidLoans()
            .filter { it.dueAt < now.time && (it.dueTotal - it.repaidAmount) > 0.0001 }

        for (loan in loans) {
            val borrower = resolveUser(loan.borrowerQq) ?: continue
            val outstanding = (loan.dueTotal - loan.repaidAmount).coerceAtLeast(0.0)
            if (outstanding <= 0.0001) continue

            // 优先从主银行扣款，再从钱包扣款。
            var remaining = outstanding
            val bankBal = EconomyUtil.getMoneyByBank(borrower)
            val takeBank = bankBal.coerceAtMost(remaining)
            if (takeBank > 0) {
                if (EconomyUtil.plusMoneyToBank(borrower, -takeBank)) {
                    remaining -= takeBank
                }
            }

            if (remaining > 0) {
                val walletBal = EconomyUtil.getMoneyByUser(borrower)
                val takeWallet = walletBal.coerceAtMost(remaining)
                if (takeWallet > 0) {
                    if (EconomyUtil.minusMoneyToUser(borrower, takeWallet)) {
                        remaining -= takeWallet
                    }
                }
            }

            val paid = ShareUtils.rounding(outstanding - remaining)
            if (paid <= 0) continue

            EconomyUtil.plusMoneyToPluginBankForId(loan.bankCode, PrivateBankLedger.LIQUIDITY_DESC, paid)
            loan.repaidAmount = ShareUtils.rounding(loan.repaidAmount + paid)
            if (loan.repaidAmount + 0.0001 >= loan.dueTotal) {
                loan.repaidAt = System.currentTimeMillis()
            }
            PrivateBankRepository.saveLoan(loan)
            processed++
        }
        return processed
    }
}
