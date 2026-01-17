package cn.chahuyun.economy.privatebank

import cn.chahuyun.economy.entity.bank.BankInfo
import cn.chahuyun.economy.entity.privatebank.*
import cn.chahuyun.economy.utils.*
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.User
import java.util.*
import kotlin.math.ln

object PrivateBankService {

    /** 创建门槛（主银行余额） */
    const val CREATE_THRESHOLD = 100_000_000.0

    /** 创建时扣除并沉淀到私银资产的启动资金 */
    const val CREATE_STARTUP_AMOUNT = 100_000_000.0

    private const val RESERVE_RATIO = 0.8
    private const val DEFAULT_STAR = 3
    private const val LOAN_TERM_DAYS = 14

    private fun baseInterest(): Int {
        val bankInfo = HibernateFactory.selectOneById(BankInfo::class.java, 1)
        return bankInfo?.interest ?: 0
    }

    /** 主银行基准利率（百分比值，例如 0.8 表示 0.8%） */
    fun baseInterestPercent(): Double = baseInterest() / 10.0

    private fun allowedInterestRange(base: Int): IntRange {
        // ±0.3% -> interest 量纲为 0.1%，因此 ±3
        return (base - 3)..(base + 3)
    }

    private fun findBankByCodeOrName(key: String): PrivateBank? {
        val trimmed = key.trim()
        if (trimmed.isBlank()) return null
        return PrivateBankRepository.findBankByCode(trimmed)
            ?: PrivateBankRepository.listBanks().firstOrNull { it.name == trimmed }
    }

    fun getBank(key: String): PrivateBank? = findBankByCodeOrName(key)

    fun borrowFromBank(user: User, bankKey: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val bank = findBankByCodeOrName(bankKey) ?: return false to "未找到该银行：$bankKey"
        val offer = PrivateBankRepository.listLoanOffers(bank.code)
            .filter { it.enabled && it.remaining >= amount }
            .minByOrNull { it.interest }
            ?: return false to "该银行暂无可用贷款额度"
        return borrow(user, offer.id, amount)
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
            return false to "创建失败：你已拥有自己的银行（code=${existingOwnerBank.code}）"
        }

        val bankBalance = EconomyUtil.getMoneyByBank(owner)
        if (bankBalance < CREATE_THRESHOLD) {
            return false to "创建失败：主银行余额需达到 100,000,000（当前 ${MoneyFormatUtil.format(bankBalance)}）"
        }

        val code = (codeInput ?: "").trim().ifBlank { generateBankCode(owner.id) }
        if (!validateBankCode(code)) {
            return false to "创建失败：银行 code 仅允许字母数字及 -_，长度 3-48"
        }
        if (PrivateBankRepository.findBankByCode(code) != null) {
            return false to "创建失败：该 code 已被占用：$code"
        }

        val bank = PrivateBank(
            code = code,
            name = name,
            ownerQq = owner.id,
            depositorInterest = baseInterest(),
            star = DEFAULT_STAR
        )

        val reservePart = ShareUtils.rounding(CREATE_STARTUP_AMOUNT * RESERVE_RATIO)
        val liquidityPart = ShareUtils.rounding(CREATE_STARTUP_AMOUNT - reservePart)

        // 1) 启动资金 80%：从行长主银行 -> 私银在主银行的准备金子账户
        if (!EconomyUtil.turnUserGlobalBankToGlobalBankAccount(owner, bank.code, PrivateBankLedger.RESERVE_DESC, reservePart)) {
            return false to "创建失败：启动资金划转(准备金)失败（请确认主银行余额足够且经济服务可用）"
        }

        // 2) 启动资金 20%：从行长主银行 -> 私银自定义账本的流动金池
        if (!EconomyUtil.turnUserGlobalBankToPluginBankForId(owner, bank.code, PrivateBankLedger.LIQUIDITY_DESC, liquidityPart)) {
            // 回滚准备金
            EconomyUtil.plusMoneyToBankFromId(bank.code, PrivateBankLedger.RESERVE_DESC, -reservePart)
            EconomyUtil.plusMoneyToBank(owner, reservePart)
            return false to "创建失败：启动资金划转(流动金)失败"
        }

        PrivateBankRepository.saveBank(bank)
        return true to "创建成功：${bank.name}（code=${bank.code}）启动资金 100,000,000 已入账（准备金 ${MoneyFormatUtil.format(reservePart)} / 流动金 ${MoneyFormatUtil.format(liquidityPart)}）"
    }

    fun deposit(user: User, bankCode: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"

        val bank = findBankByCodeOrName(bankCode) ?: return false to "未找到该银行：$bankCode"
        if (bank.vipOnly) {
            val list = (bank.vipWhitelist ?: "").split(',').mapNotNull { it.trim().takeIf(String::isNotBlank) }
            if (list.isNotEmpty() && user.id.toString() !in list) {
                return false to "该银行仅对 VIP 开放"
            }
        }

        val wallet = EconomyUtil.getMoneyByUser(user)
        if (wallet - amount < 0) return false to "钱包余额不足（当前 ${MoneyFormatUtil.format(wallet)}）"

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

        return true to "存入成功：${MoneyFormatUtil.format(amount)}（准备金 ${MoneyFormatUtil.format(reservePart)} / 流动金 ${MoneyFormatUtil.format(liquidityPart)}）"
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
            deposit.updatedAt = Date()
            PrivateBankRepository.saveDeposit(deposit)
            refreshRating(bank.code)
            true to "取款成功：${MoneyFormatUtil.format(amount)}"
        } else {
            bank.withdrawFailures += 1
            bank.defaulterUntil = DateUtil.offsetDay(Date(), 30)
            bank.depositorInterest = baseInterest() // 失信期间强制同步主银行利率
            PrivateBankRepository.saveBank(bank)
            refreshRating(bank.code)
            false to "取款失败：该银行资金链不足，已标记为失信银行（30天）"
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
            Log.error("银行:强制赎回国卷失败", e)
            null
        }
    }

    fun addReview(user: User, bankCode: String, rating: Int): Pair<Boolean, String> {
        return addReview(user, bankCode, rating, null)
    }

    fun addReview(user: User, bankCode: String, rating: Int, content: String?): Pair<Boolean, String> {
        if (rating !in 1..5) return false to "评分必须在 1-5"
        val bank = findBankByCodeOrName(bankCode) ?: return false to "未找到该银行：$bankCode"

        val deposit = PrivateBankRepository.findDeposit(bank.code, user.id)
            ?: return false to "评分失败：你在该银行没有存款"

        val days = DateUtil.between(deposit.createdAt, Date(), DateUnit.DAY)
        if (days < 7) {
            return false to "评分失败：存款需满 7 天后才可评分（当前 ${days} 天）"
        }

        val now = Date()
        val recentCount = PrivateBankRepository.listReviewsByUser(bank.code, user.id)
            .count { DateUtil.between(it.createdAt, now, DateUnit.DAY) < 30 }
        if (recentCount >= 2) {
            return false to "评分失败：一个月内最多评分 2 次"
        }

        val clean = content?.trim()?.takeIf { it.isNotBlank() }?.take(500)
        PrivateBankRepository.addReview(
            PrivateBankReview(
                bankCode = bank.code,
                userQq = user.id,
                rating = rating,
                content = clean
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
                // 支持输入 1.2（百分比）或 12（interest 整数）
                val v = key.toDoubleOrNull() ?: return false to "利率参数错误：请填 rate/max/min/now"
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
            return false to "利率超出允许范围：主银行 ${FormatUtil.fixed(base / 10.0, 1)}% 的 ±0.3%（允许 ${FormatUtil.fixed(allowed.first / 10.0, 1)}% - ${FormatUtil.fixed(allowed.last / 10.0, 1)}%）"
        }

        bank.depositorInterest = nextInterest
        PrivateBankRepository.saveBank(bank)
        refreshRating(bank.code)
        return true to "设置成功：当前利率 ${FormatUtil.fixed(bank.depositorInterest / 10.0, 1)}%"
    }

    private fun maxLoanOffersByStar(star: Int): Int = star.coerceIn(1, 5)

    private fun maxLoanInterestByStar(star: Int): Int {
        // interest 量纲为 0.1%，这里给一个随星级提升的上限
        return when (star.coerceIn(1, 5)) {
            1 -> 25
            2 -> 35
            3 -> 45
            4 -> 55
            else -> 65
        }
    }

    fun publishLoanByPlan(owner: User, bankCode: String, total: Double, ratePercent: Double): Pair<Boolean, String> {
        val interest = (ratePercent * 10.0).toInt()
        return publishLoan(owner, bankCode, total, interest, LOAN_TERM_DAYS, "LIQUIDITY")
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
        val bank = PrivateBankRepository.findBankByCode(bankCode) ?: return false to "未找到该银行：$bankCode"
        if (bank.ownerQq != owner.id) return false to "只有行长可以购买国卷"

        val issue = ensureWeeklyBondIssue()
        if (issue.remaining < amount) return false to "本周国卷剩余额度不足（剩余 ${MoneyFormatUtil.format(issue.remaining)}）"

        val liquidity = EconomyUtil.getMoneyFromPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC)
        if (liquidity < amount) return false to "流动金池余额不足（当前 ${MoneyFormatUtil.format(liquidity)}）"

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

        return true to "购买成功：国卷 ${MoneyFormatUtil.format(amount)}（锁仓 ${issue.lockDays} 天，倍数 ${FormatUtil.fixed(issue.rateMultiplier, 2)}x）"
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

        return true to "赎回成功：到账 ${MoneyFormatUtil.format(payout)}"
    }

    fun publishLoan(owner: User, bankCode: String, total: Double, interest: Int, termDays: Int, source: String): Pair<Boolean, String> {
        if (total <= 0) return false to "金额必须大于 0"
        if (termDays !in 1..30) return false to "期限仅支持 1-30 天"

        val bank = findBankByCodeOrName(bankCode) ?: return false to "未找到该银行：$bankCode"
        if (bank.ownerQq != owner.id) return false to "只有行长可以发布贷款"

        val activeOffers = PrivateBankRepository.listLoanOffers(bank.code)
            .count { it.enabled && it.remaining > 0.0001 }
        if (activeOffers >= maxLoanOffersByStar(bank.star)) {
            return false to "发布失败：当前银行星级最多允许 ${maxLoanOffersByStar(bank.star)} 笔放贷标的"
        }

        if (interest <= 0 || interest > maxLoanInterestByStar(bank.star)) {
            return false to "发布失败：利息超出星级限制（⭐${bank.star} 最大允许 ${FormatUtil.fixed(maxLoanInterestByStar(bank.star) / 10.0, 1)}%）"
        }

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
        return true to "发布成功：offerId=${offer.id} 可借 ${MoneyFormatUtil.format(offer.remaining)}"
    }

    fun borrow(user: User, offerId: Int, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val offer = PrivateBankRepository.findLoanOffer(offerId) ?: return false to "未找到该贷款标的"
        if (!offer.enabled) return false to "该贷款标的已关闭"
        if (offer.remaining < amount) return false to "剩余额度不足（剩余 ${MoneyFormatUtil.format(offer.remaining)}）"

        // 计划口径：贷款金额进入借款人在该银行的“存款账户”（用户可自行取款使用）
        if (!EconomyUtil.plusMoneyToPluginBankForId(offer.bankCode, PrivateBankLedger.INVENTORY_DESC, -amount)) {
            return false to "放款失败：放贷仓库扣减失败"
        }

        // 把资金转入流动金池以覆盖可能的取款（资产形态从放贷仓库变为流动金）
        if (!EconomyUtil.plusMoneyToPluginBankForId(offer.bankCode, PrivateBankLedger.LIQUIDITY_DESC, amount)) {
            EconomyUtil.plusMoneyToPluginBankForId(offer.bankCode, PrivateBankLedger.INVENTORY_DESC, amount)
            return false to "放款失败：流动金池入账失败"
        }

        val dep = PrivateBankRepository.findDeposit(offer.bankCode, user.id)
            ?: PrivateBankDeposit(bankCode = offer.bankCode, userQq = user.id, principal = 0.0)
        dep.principal = ShareUtils.rounding(dep.principal + amount)
        dep.updatedAt = Date()
        PrivateBankRepository.saveDeposit(dep)

        offer.remaining = ShareUtils.rounding(offer.remaining - amount)
        PrivateBankRepository.saveLoanOffer(offer)

        val dueAt = DateUtil.offsetDay(Date(), offer.termDays)
        val dueTotal = ShareUtils.rounding(amount * (1 + (offer.interest / 1000.0) * offer.termDays))
        val loan = PrivateBankLoan(
            offerId = offer.id,
            bankCode = offer.bankCode,
            lenderQq = offer.ownerQq,
            borrowerQq = user.id,
            principal = amount,
            dueTotal = dueTotal,
            repaidAmount = 0.0,
            interest = offer.interest,
            termDays = offer.termDays,
            createdAt = Date(),
            dueAt = dueAt
        )
        PrivateBankRepository.saveLoan(loan)

        return true to "借款成功：loanId=${loan.id} 金额=${MoneyFormatUtil.format(amount)} 应还=${MoneyFormatUtil.format(dueTotal)} 到期日 ${DateUtil.formatDateTime(dueAt)}"
    }

    fun repay(user: User, loanId: Int): Pair<Boolean, String> {
        val loan = PrivateBankRepository.findLoan(loanId) ?: return false to "未找到借款单"
        if (loan.borrowerQq != user.id) return false to "只能偿还自己的借款"
        if (loan.repaidAt != null) return false to "该借款已结清"
        val outstanding = ShareUtils.rounding((loan.dueTotal - loan.repaidAmount).coerceAtLeast(0.0))
        return repayByAmount(user, loanId, outstanding)
    }

    fun repayByAmount(user: User, loanId: Int, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val loan = PrivateBankRepository.findLoan(loanId) ?: return false to "未找到借款单"
        if (loan.borrowerQq != user.id) return false to "只能偿还自己的借款"
        if (loan.repaidAt != null) return false to "该借款已结清"

        val outstanding = (loan.dueTotal - loan.repaidAmount).coerceAtLeast(0.0)
        if (outstanding <= 0.0001) {
            loan.repaidAt = Date()
            PrivateBankRepository.saveLoan(loan)
            return true to "该借款已结清"
        }

        val pay = amount.coerceAtMost(outstanding)

        // 扣款：优先钱包，不足再扣主银行
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
            loan.repaidAt = Date()
        }
        PrivateBankRepository.saveLoan(loan)

        val left = ShareUtils.rounding((loan.dueTotal - loan.repaidAmount).coerceAtLeast(0.0))
        return true to "还款成功：已还 ${MoneyFormatUtil.format(paid)}，剩余 ${MoneyFormatUtil.format(left)}"
    }

    fun repayToBankByAmount(user: User, bankCode: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val bank = findBankByCodeOrName(bankCode) ?: return false to "未找到该银行：$bankCode"
        val loans = PrivateBankRepository.listLoansByBorrower(user.id)
            .filter { it.repaidAt == null && it.bankCode == bank.code }
            .sortedBy { it.createdAt.time }

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
     * 到期自动扣款：优先主银行，其次钱包；扣到的钱进入私银流动金池。
     * 可能因为用户离线/无法解析 User 对象而跳过。
     */
    fun collectOverdueLoans(): Int {
        val now = Date()
        var processed = 0
        val loans = PrivateBankRepository.listUnrepaidLoans()
            .filter { it.dueAt.before(now) && (it.dueTotal - it.repaidAmount) > 0.0001 }

        for (loan in loans) {
            val borrower = resolveUser(loan.borrowerQq) ?: continue
            val outstanding = (loan.dueTotal - loan.repaidAmount).coerceAtLeast(0.0)
            if (outstanding <= 0.0001) continue

            // 自动扣：优先主银行
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
                loan.repaidAt = Date()
            }
            PrivateBankRepository.saveLoan(loan)
            processed++
        }
        return processed
    }
}
