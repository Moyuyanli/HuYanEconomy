package cn.chahuyun.economy.privatebank

import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.data.repository.PrivateBankRepository
import cn.chahuyun.economy.manager.BankManager
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.privatebank.*
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.utils.*
import cn.hutool.core.date.DateUnit
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.User
import xyz.cssxsh.mirai.economy.EconomyService
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
    private const val MIN_RESERVE_AFTER_WITHDRAW = 80_000_000.0
    private const val MIN_LIQUIDITY_AFTER_WITHDRAW = 20_000_000.0
    private const val GOV_BOND_PUBLISH_HOUR = 10
    private const val GOV_BOND_PUBLISH_MINUTE = 30
    private const val DAY_MILLIS = 86_400_000L
    private const val BOND_EXPIRING_WINDOW_MILLIS = DAY_MILLIS

    private fun baseInterest(): Int {
        return BankManager.getBankInfo(1)?.interest ?: 0
    }

    /** 主银行基准利率，返回百分比数值，例如 0.8 表示 0.8%。 */
    fun baseInterestPercent(): Double = baseInterest() / 10.0

    private fun allowedInterestRange(base: Int): IntRange {
        // 允许在主银行利率上下浮动 0.3%，interest 内部单位是 0.1%，所以偏移 3。
        return (base - 3)..(base + 3)
    }

    private fun findAnyBankByCodeOrName(key: String): PrivateBankDto? {
        val trimmed = key.trim()
        if (trimmed.isBlank()) return null
        return PrivateBankRepository.findBankByCode(trimmed)
            ?: PrivateBankRepository.listBanks().firstOrNull { it.name == trimmed }
    }

    private fun findBankByCodeOrName(key: String): PrivateBankDto? {
        val bank = findAnyBankByCodeOrName(key) ?: return null
        PrivateBankBankruptcyService.evaluate(bank.code)
        return PrivateBankRepository.findBankByCode(bank.code)?.takeUnless { it.isBankrupt() }
    }

    private fun findOwnedActiveBank(ownerQq: Long): PrivateBankDto? {
        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == ownerQq } ?: return null
        PrivateBankBankruptcyService.evaluate(bank.code)
        return PrivateBankRepository.findBankByCode(bank.code)?.takeUnless { it.isBankrupt() }
    }

    fun getBank(key: String): PrivateBankDto? {
        val bank = findAnyBankByCodeOrName(key) ?: return null
        PrivateBankBankruptcyService.evaluate(bank.code)
        return PrivateBankRepository.findBankByCode(bank.code)
    }

    fun getDeposit(bankCode: String, userQq: Long): PrivateBankDepositDto? =
        PrivateBankRepository.findDeposit(bankCode, userQq)

    fun borrowFromBank(user: User, bankKey: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        validateBorrowerEligibility(user)?.let { return false to it }
        val bank = findBankByCodeOrName(bankKey) ?: return false to "未找到该银行：$bankKey"
        val offer = selectBestLoanOffer(PrivateBankRepository.listLoanOffers(bank.code), amount)
            ?: return false to "该银行暂无可用贷款额度"
        return borrow(user, offer.id, amount)
    }

    private fun validateBorrowerEligibility(user: User): String? {
        val wallet = EconomyUtil.getMoneyByUser(user)
        if (wallet < 0.0) {
            return "你的钱包余额为负，暂时不能贷款"
        }

        val userInfo = UserCoreManager.getUserInfo(user)
        if (userInfo.signNumber <= 7) {
            return "连续签到天数需大于 7 天才可以贷款"
        }

        return null
    }

    fun canUserDepositToBank(userQq: Long, bank: PrivateBankDto): Boolean =
        bank.ownerQq != userQq

    fun isLoanInterestAllowed(interest: Int): Boolean =
        interest in MIN_LOAN_INTEREST..MAX_LOAN_INTEREST

    fun loanInterestRangeText(): String =
        "${FormatUtil.fixed(MIN_LOAN_INTEREST / 10.0, 1)}%-${FormatUtil.fixed(MAX_LOAN_INTEREST / 10.0, 1)}%"

    fun loanInterestPercentText(interest: Int): String =
        "${FormatUtil.fixed(interest / 10.0, 1).removeSuffix(".0")}%"

    fun calculateLoanDueTotal(principal: Double, interest: Int): Double =
        ShareUtils.rounding(principal * (1 + interest / 1000.0))

    private fun normalizeLoanDueTotal(loan: PrivateBankLoanDto, now: Long = System.currentTimeMillis()): Boolean {
        val expectedDueTotal = calculateLoanDueTotal(loan.principal, loan.interest)
        if (abs(loan.dueTotal - expectedDueTotal) <= 0.1) return false

        loan.dueTotal = expectedDueTotal
        if (loan.repaidAt == 0L && loan.repaidAmount + 0.0001 >= loan.dueTotal) {
            loan.repaidAt = now
        }
        return true
    }

    fun selectBestLoanOffer(offers: List<PrivateBankLoanOfferDto>, amount: Double): PrivateBankLoanOfferDto? =
        offers.asSequence()
            .filter { it.enabled && it.remaining >= amount }
            .sortedWith(
                compareBy<PrivateBankLoanOfferDto> { it.interest }
                    .thenBy { it.createdAt }
                    .thenBy { it.id }
            )
            .firstOrNull()

    data class LoanIncomeRepayment(
        val repaid: Double,
        val remaining: Double,
    )

    fun hasUnrepaidLoans(userQq: Long): Boolean =
        PrivateBankRepository.listLoansByBorrower(userQq)
            .any { it.repaidAt == 0L && (it.dueTotal - it.repaidAmount) > 0.0001 }

    fun hasOverdueLoans(userQq: Long, now: Long = System.currentTimeMillis()): Boolean =
        PrivateBankRepository.listLoansByBorrower(userQq)
            .any { it.repaidAt == 0L && it.dueAt < now && (it.dueTotal - it.repaidAmount) > 0.0001 }

    fun repayOverdueLoansFromIncome(userQq: Long, income: Double, now: Long = System.currentTimeMillis()): LoanIncomeRepayment {
        if (income <= 0.0) return LoanIncomeRepayment(0.0, 0.0)

        var remaining = ShareUtils.rounding(income)
        var repaid = 0.0
        val loans = PrivateBankRepository.listLoansByBorrower(userQq)
            .filter { it.repaidAt == 0L && it.dueAt < now && (it.dueTotal - it.repaidAmount) > 0.0001 }
            .sortedBy { it.dueAt }

        for (loan in loans) {
            if (remaining <= 0.0001) break
            if (normalizeLoanDueTotal(loan, now)) {
                PrivateBankRepository.saveLoan(loan)
            }
            if (loan.repaidAt != 0L) continue
            val outstanding = ShareUtils.rounding((loan.dueTotal - loan.repaidAmount).coerceAtLeast(0.0))
            if (outstanding <= 0.0001) continue

            val pay = remaining.coerceAtMost(outstanding)
            if (!PrivateBankLedger.add(loan.bankCode, PrivateBankLedger.LIQUIDITY_DESC, pay)) {
                break
            }

            loan.repaidAmount = ShareUtils.rounding(loan.repaidAmount + pay)
            if (loan.repaidAmount + 0.0001 >= loan.dueTotal) {
                loan.repaidAt = now
            }
            PrivateBankRepository.saveLoan(loan)
            repaid = ShareUtils.rounding(repaid + pay)
            remaining = ShareUtils.rounding(remaining - pay)
        }

        return LoanIncomeRepayment(repaid, remaining)
    }

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
        if (hasAnyHistoricalPrivateBankData(code)) {
            return false to "创建失败：code=$code 已存在历史私人银行数据，请换一个 code 或先执行 hye repair privatebank ledger $code 清理孤儿账本"
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
        if (!PrivateBankLedger.transferFromMainBank(owner, bank.code, PrivateBankLedger.RESERVE_DESC, reservePart)) {
            return false to "创建失败：启动资金划转准备金失败"
        }

        // 2) 20% 进入流动金池，存放在插件自定义账户中。
        if (!PrivateBankLedger.transferFromMainBank(owner, bank.code, PrivateBankLedger.LIQUIDITY_DESC, liquidityPart)) {
            // 回滚准备金划转
            PrivateBankLedger.add(bank.code, PrivateBankLedger.RESERVE_DESC, -reservePart)
            EconomyUtil.plusMoneyToBank(owner, reservePart)
            return false to "创建失败：启动资金划转流动金失败"
        }

        PrivateBankRepository.saveBank(bank)
        return true to "创建成功：${bank.name}(code=${bank.code}) 启动资金 100,000,000 已入账（准备金 ${MoneyFormatUtil.format(reservePart)} / 流动金 ${MoneyFormatUtil.format(liquidityPart)}）"
    }

    enum class FundingPool(val code: String, val displayName: String) {
        RESERVE("P", "准备金池"),
        LIQUIDITY("F", "流动金池");

        companion object {
            fun parse(raw: String?): FundingPool? {
                return when (raw?.trim()?.uppercase(Locale.getDefault()).orEmpty().ifBlank { "P" }) {
                    "P" -> RESERVE
                    "F" -> LIQUIDITY
                    else -> null
                }
            }
        }
    }

    enum class FundingSource(val displayName: String) {
        WALLET("钱包"),
        MAIN_BANK("主银行")
    }

    data class FundingPoolTransfer(
        val from: FundingPool,
        val to: FundingPool,
    )

    fun parseFundingPoolTransfer(raw: String?): FundingPoolTransfer? {
        val code = raw?.trim()?.uppercase(Locale.getDefault()).orEmpty()
        if (code.length != 2) return null
        val from = FundingPool.parse(code[0].toString()) ?: return null
        val to = FundingPool.parse(code[1].toString()) ?: return null
        return FundingPoolTransfer(from, to).takeIf { from != to }
    }

    fun selectFundingSource(amount: Double, walletBalance: Double, mainBankBalance: Double): FundingSource? {
        if (amount <= 0) return null
        return when {
            walletBalance + 1e-6 >= amount -> FundingSource.WALLET
            mainBankBalance + 1e-6 >= amount -> FundingSource.MAIN_BANK
            else -> null
        }
    }

    fun fund(owner: User, amount: Double, pool: FundingPool): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val bank = findOwnedActiveBank(owner.id) ?: return false to "你的银行不存在或已经破产"
        val walletBalance = EconomyUtil.getMoneyByUser(owner)
        val mainBankBalance = EconomyUtil.getMoneyByBank(owner)
        val source = selectFundingSource(amount, walletBalance, mainBankBalance)
        if (source == null) {
            return false to "补资失败：钱包和主银行均无法单独支付 ${MoneyFormatUtil.format(amount)}" +
                "（钱包 ${MoneyFormatUtil.format(walletBalance)} / 主银行 ${MoneyFormatUtil.format(mainBankBalance)}）"
        }

        val description = when (pool) {
            FundingPool.RESERVE -> PrivateBankLedger.RESERVE_DESC
            FundingPool.LIQUIDITY -> PrivateBankLedger.LIQUIDITY_DESC
        }
        val ok = when (source) {
            FundingSource.WALLET -> PrivateBankLedger.transferFromWallet(owner, bank.code, description, amount)
            FundingSource.MAIN_BANK -> PrivateBankLedger.transferFromMainBank(owner, bank.code, description, amount)
        }

        if (!ok) return false to "补资失败：资金划转失败"
        return true to "补资成功：从${source.displayName}划转 ${MoneyFormatUtil.format(amount)} 到 ${bank.name} 的${pool.displayName}"
    }

    fun transferFundingPool(owner: User, amount: Double, transfer: FundingPoolTransfer): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val bank = findOwnedActiveBank(owner.id) ?: return false to "你的银行不存在或已经破产"
        val fromDescription = fundingPoolDescription(transfer.from)
        val toDescription = fundingPoolDescription(transfer.to)
        val current = PrivateBankLedger.balance(bank.code, fromDescription)
        if (current + 1e-6 < amount) {
            return false to "${transfer.from.displayName}余额不足（当前 ${MoneyFormatUtil.format(current)}）"
        }
        if (!PrivateBankLedger.debit(bank.code, fromDescription, amount)) {
            return false to "池间转移失败：${transfer.from.displayName}扣减失败"
        }
        if (!PrivateBankLedger.add(bank.code, toDescription, amount)) {
            PrivateBankLedger.add(bank.code, fromDescription, amount)
            return false to "池间转移失败：${transfer.to.displayName}入账失败，来源资金已回滚"
        }
        return true to "池间转移成功：${MoneyFormatUtil.format(amount)} 已从${transfer.from.displayName}转入${transfer.to.displayName}"
    }

    private fun fundingPoolDescription(pool: FundingPool): String = when (pool) {
        FundingPool.RESERVE -> PrivateBankLedger.RESERVE_DESC
        FundingPool.LIQUIDITY -> PrivateBankLedger.LIQUIDITY_DESC
    }

    fun divest(owner: User, amount: Double, pool: FundingPool): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val bank = findOwnedActiveBank(owner.id) ?: return false to "你的银行不存在或已经破产"

        val current = when (pool) {
            FundingPool.RESERVE -> PrivateBankLedger.balance(bank.code, PrivateBankLedger.RESERVE_DESC)
            FundingPool.LIQUIDITY -> PrivateBankLedger.balance(bank.code, PrivateBankLedger.LIQUIDITY_DESC)
        }
        val minLeft = when (pool) {
            FundingPool.RESERVE -> MIN_RESERVE_AFTER_WITHDRAW
            FundingPool.LIQUIDITY -> MIN_LIQUIDITY_AFTER_WITHDRAW
        }
        if (current < amount) {
            return false to "${pool.displayName}余额不足（当前 ${MoneyFormatUtil.format(current)}）"
        }
        if (current - amount < minLeft) {
            return false to "撤资失败：${pool.displayName}撤资后不得低于 ${MoneyFormatUtil.format(minLeft)}"
        }

        val debitOk = when (pool) {
            FundingPool.RESERVE -> PrivateBankLedger.debit(bank.code, PrivateBankLedger.RESERVE_DESC, amount)
            FundingPool.LIQUIDITY -> PrivateBankLedger.debit(bank.code, PrivateBankLedger.LIQUIDITY_DESC, amount)
        }
        if (!debitOk) return false to "撤资失败：资金扣减失败"

        if (!EconomyUtil.plusMoneyToBank(owner, amount)) {
            when (pool) {
                FundingPool.RESERVE -> PrivateBankLedger.add(bank.code, PrivateBankLedger.RESERVE_DESC, amount)
                FundingPool.LIQUIDITY -> PrivateBankLedger.add(bank.code, PrivateBankLedger.LIQUIDITY_DESC, amount)
            }
            return false to "撤资失败：回款到主银行失败"
        }

        return true to "撤资成功：${MoneyFormatUtil.format(amount)} 已从 ${pool.displayName} 回到你的主银行"
    }

    fun updateBankProfile(owner: User, newCodeRaw: String, newNameRaw: String): Pair<Boolean, String> {
        val bank = findOwnedActiveBank(owner.id) ?: return false to "你的银行不存在或已经破产"
        val newCode = newCodeRaw.trim()
        val newName = newNameRaw.trim()
        if (!validateBankCode(newCode)) {
            return false to "修改失败：code 只能包含字母、数字、-、_，长度 3-48"
        }
        if (newName.isBlank() || newName.length > 128) {
            return false to "修改失败：银行名称不能为空，且不能超过 128 个字符"
        }

        val oldCode = bank.code
        if (!newCode.equals(oldCode, ignoreCase = false)) {
            val existing = PrivateBankRepository.findBankByCode(newCode)
            if (existing != null && existing.id != bank.id) {
                return false to "修改失败：code 已存在：$newCode"
            }
            if (hasAnyHistoricalPrivateBankData(newCode)) {
                return false to "修改失败：目标 code=$newCode 已存在历史私人银行数据，请换一个 code 或先处理旧数据"
            }
            migrateBankCodeReferences(oldCode, newCode)
        }

        bank.code = newCode
        bank.name = newName
        PrivateBankRepository.saveBank(bank)
        return true to "银行资料已更新：${bank.name}(code=${bank.code})"
    }

    fun deposit(user: User, bankCode: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"

        val bank = findBankByCodeOrName(bankCode) ?: return false to "未找到该银行：$bankCode"
        return PrivateBankLocks.withBankLock(bank.code) {
            depositLocked(user, bank, amount)
        }
    }

    private fun depositLocked(user: User, bank: PrivateBankDto, amount: Double): Pair<Boolean, String> {
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

        if (!EconomyUtil.minusMoneyToUser(user, amount)) {
            return false to "存入失败：钱包扣减失败"
        }

        val repayment = runCatching { PrivateBankDebtService.repay(bank.code, amount) }.getOrElse {
            EconomyUtil.plusMoneyToUser(user, amount)
            return false to "存入失败：主银行债务结算失败"
        }
        val depositedAmount = repayment.remainingInput

        if (!creditDepositPools(bank.code, depositedAmount)) {
            if (depositedAmount > 0) EconomyUtil.plusMoneyToUser(user, depositedAmount)
            val repaid = ShareUtils.rounding(repayment.paidInterest + repayment.paidPrincipal)
            return if (repaid > 0) {
                refreshRating(bank.code)
                true to "存入部分完成：${MoneyFormatUtil.format(repaid)} 已偿还主银行债务，" +
                    "${MoneyFormatUtil.format(depositedAmount)} 入池失败并已退回钱包"
            } else {
                false to "存入失败：资金池入账失败，金额已退回钱包"
            }
        }

        if (depositedAmount > 0.0001) {
            val deposit = PrivateBankRepository.findDeposit(bank.code, user.id) ?: PrivateBankDepositDto(
                bankCode = bank.code,
                userQq = user.id,
                principal = 0.0
            )
            deposit.principal = ShareUtils.rounding(deposit.principal + depositedAmount)
            deposit.updatedAt = System.currentTimeMillis()
            PrivateBankRepository.saveDeposit(deposit)
        }

        refreshRating(bank.code)
        val repaid = ShareUtils.rounding(repayment.paidInterest + repayment.paidPrincipal)
        return if (repaid > 0.0001) {
            true to buildString {
                append("存入完成：原始金额 ${MoneyFormatUtil.format(amount)}")
                append("，偿还利息 ${MoneyFormatUtil.format(repayment.paidInterest)}")
                append("，偿还本金 ${MoneyFormatUtil.format(repayment.paidPrincipal)}")
                append("，实际新增存款 ${MoneyFormatUtil.format(depositedAmount)}")
                append("，剩余主银行债务 ${MoneyFormatUtil.format(repayment.remainingDebt)}")
            }
        } else {
            true to "存入成功：${MoneyFormatUtil.format(depositedAmount)} 已存入 ${bank.name}(code=${bank.code})"
        }
    }

    private fun creditDepositPools(bankCode: String, amount: Double): Boolean {
        if (amount <= 0.0001) return true

        val reservePart = ShareUtils.rounding(amount * RESERVE_RATIO)
        val liquidityPart = ShareUtils.rounding(amount - reservePart)
        if (reservePart > 0 && !PrivateBankLedger.add(bankCode, PrivateBankLedger.RESERVE_DESC, reservePart)) return false
        if (liquidityPart > 0 && !PrivateBankLedger.add(bankCode, PrivateBankLedger.LIQUIDITY_DESC, liquidityPart)) {
            if (reservePart > 0) PrivateBankLedger.debit(bankCode, PrivateBankLedger.RESERVE_DESC, reservePart)
            return false
        }
        return true
    }

    fun withdraw(user: User, bankCode: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val bank = findBankByCodeOrName(bankCode) ?: return false to "未找到该银行：$bankCode"

        return PrivateBankLocks.withBankLock(bank.code) {
            withdrawLocked(user, bank, amount)
        }
    }

    data class WithdrawalSources(
        val liquidity: Double = 0.0,
        val reserve: Double = 0.0,
        val loanInventory: Double = 0.0,
        val ownerMainBank: Double = 0.0,
        val mainBankLoan: Double = 0.0,
    ) {
        val total: Double
            get() = ShareUtils.rounding(liquidity + reserve + loanInventory + ownerMainBank + mainBankLoan)
    }

    data class WithdrawalSettlement(
        val requested: Double,
        val sources: WithdrawalSources,
    ) {
        val paid: Double = sources.total.coerceAtMost(requested)
        val unpaid: Double = ShareUtils.rounding((requested - paid).coerceAtLeast(0.0))
        val complete: Boolean = unpaid <= 0.0001
    }

    data class LoanOfferReclaim(val offerId: Int, val amount: Double)

    fun buildLoanOfferReclaimPlan(
        offers: List<PrivateBankLoanOfferDto>,
        inventory: Double,
        requested: Double,
    ): List<LoanOfferReclaim> {
        var remaining = requested.coerceAtMost(inventory.coerceAtLeast(0.0))
        if (remaining <= 0.0001) return emptyList()
        val plan = mutableListOf<LoanOfferReclaim>()
        offers.asSequence()
            .filter { it.enabled && it.remaining > 0.0001 }
            .sortedWith(compareByDescending<PrivateBankLoanOfferDto> { it.interest }.thenBy { it.createdAt }.thenBy { it.id })
            .forEach { offer ->
                if (remaining <= 0.0001) return@forEach
                val take = ShareUtils.rounding(offer.remaining.coerceAtMost(remaining))
                if (take > 0.0001) {
                    plan += LoanOfferReclaim(offer.id, take)
                    remaining = ShareUtils.rounding(remaining - take)
                }
            }
        return plan
    }

    private fun withdrawLocked(user: User, bank: PrivateBankDto, amount: Double): Pair<Boolean, String> {

        val deposit = PrivateBankRepository.findDeposit(bank.code, user.id)
            ?: return false to "你在该银行没有存款"

        if (deposit.principal - amount < 0) {
            return false to "取款失败：你的存款不足（当前 ${MoneyFormatUtil.format(deposit.principal)}）"
        }

        bank.withdrawRequests += 1
        PrivateBankRepository.saveBank(bank)

        val wasDefaulter = bank.isDefaulter()
        val settlement = forcePayToUserWallet(bank, amount, user, wasDefaulter)
        if (settlement.paid > 0.0001) {
            deposit.principal = ShareUtils.rounding((deposit.principal - settlement.paid).coerceAtLeast(0.0))
            deposit.updatedAt = System.currentTimeMillis()
            PrivateBankRepository.saveDeposit(deposit)
        }

        return if (settlement.complete) {
            refreshRating(bank.code)
            val bankruptcy = PrivateBankBankruptcyService.evaluate(bank.code)
            true to buildString {
                append("取款成功：申请 ${MoneyFormatUtil.format(amount)}，已取出 ${MoneyFormatUtil.format(settlement.paid)}")
                if (settlement.sources.mainBankLoan > 0.0001) {
                    append("，其中主银行应急贷款 ${MoneyFormatUtil.format(settlement.sources.mainBankLoan)}")
                }
                if (bankruptcy.bankrupt) append("；银行债务超过 1G，已破产清算")
            }
        } else {
            bank.withdrawFailures += 1
            markDefaulter(bank)
            refreshRating(bank.code)
            false to "取款失败：申请 ${MoneyFormatUtil.format(amount)}，已成功取出 ${MoneyFormatUtil.format(settlement.paid)}，" +
                "未兑付 ${MoneyFormatUtil.format(settlement.unpaid)}；银行已进入 30 天失信期"
        }
    }

    private fun forcePayToUserWallet(
        bank: PrivateBankDto,
        amount: Double,
        user: User,
        allowEmergencyFunding: Boolean,
    ): WithdrawalSettlement {
        var remaining = amount
        var liquidityPaid = 0.0
        var reservePaid = 0.0
        var inventoryPaid = 0.0
        var ownerPaid = 0.0
        var mainBankLoanPaid = 0.0

        // 1) 流动金池(custom)
        val liquidity = PrivateBankLedger.balance(bank.code, PrivateBankLedger.LIQUIDITY_DESC).coerceAtLeast(0.0)
        val takeLiquidity = liquidity.coerceAtMost(remaining)
        if (takeLiquidity > 0) {
            if (PrivateBankLedger.transferToWallet(bank.code, PrivateBankLedger.LIQUIDITY_DESC, user, takeLiquidity)) {
                liquidityPaid = takeLiquidity
                remaining = ShareUtils.rounding(remaining - takeLiquidity)
            }
        }

        // 2) 准备金池(global)
        val reserve = PrivateBankLedger.balance(bank.code, PrivateBankLedger.RESERVE_DESC).coerceAtLeast(0.0)
        val takeReserve = reserve.coerceAtMost(remaining)
        if (takeReserve > 0) {
            if (PrivateBankLedger.transferToWallet(bank.code, PrivateBankLedger.RESERVE_DESC, user, takeReserve)) {
                reservePaid = takeReserve
                remaining = ShareUtils.rounding(remaining - takeReserve)
            }
        }

        // 3) 仅回收放贷标的中尚未贷出的额度。
        if (remaining > 0.0001) {
            inventoryPaid = reclaimLoanInventory(bank.code, remaining, user)
            remaining = ShareUtils.rounding((remaining - inventoryPaid).coerceAtLeast(0.0))
        }

        if (allowEmergencyFunding && remaining > 0.0001) {
            ownerPaid = payFromOwnerMainBank(bank, user, remaining)
            remaining = ShareUtils.rounding((remaining - ownerPaid).coerceAtLeast(0.0))
        }

        if (allowEmergencyFunding && remaining > 0.0001 && EconomyUtil.plusMoneyToUser(user, remaining)) {
            val debtSaved = runCatching { PrivateBankDebtService.addPrincipal(bank.code, remaining) }.isSuccess
            if (debtSaved) {
                mainBankLoanPaid = remaining
                remaining = 0.0
            } else {
                EconomyUtil.minusMoneyToUser(user, remaining)
            }
        }

        return WithdrawalSettlement(
            requested = amount,
            sources = WithdrawalSources(liquidityPaid, reservePaid, inventoryPaid, ownerPaid, mainBankLoanPaid),
        )
    }

    private fun reclaimLoanInventory(bankCode: String, requested: Double, user: User): Double {
        val offers = PrivateBankRepository.listLoanOffers(bankCode)
        val plan = buildLoanOfferReclaimPlan(offers, PrivateBankLedger.balance(bankCode, PrivateBankLedger.INVENTORY_DESC), requested)
        val total = ShareUtils.rounding(plan.sumOf { it.amount })
        if (total <= 0.0001) return 0.0

        val originals = offers.associateBy { it.id }.mapValues { it.value.copy() }
        return try {
            plan.forEach { item ->
                val offer = offers.first { it.id == item.offerId }
                offer.remaining = ShareUtils.rounding((offer.remaining - item.amount).coerceAtLeast(0.0))
                PrivateBankRepository.saveLoanOffer(offer)
            }
            if (!PrivateBankLedger.transferToWallet(bankCode, PrivateBankLedger.INVENTORY_DESC, user, total)) {
                originals.values.filter { original -> plan.any { it.offerId == original.id } }
                    .forEach(PrivateBankRepository::saveLoanOffer)
                0.0
            } else {
                total
            }
        } catch (e: Exception) {
            originals.values.filter { original -> plan.any { it.offerId == original.id } }
                .forEach { runCatching { PrivateBankRepository.saveLoanOffer(it) } }
            Log.error("银行:回收未贷额度失败 bankCode=$bankCode", e)
            0.0
        }
    }

    private fun payFromOwnerMainBank(bank: PrivateBankDto, user: User, requested: Double): Double {
        val accountId = UserCoreManager.getUserInfo(bank.ownerQq)?.id?.takeIf { it.isNotBlank() } ?: return 0.0
        val account = EconomyService.account(accountId, null)
        val amount = EconomyUtil.getMoneyByBankForAccount(account).coerceAtLeast(0.0).coerceAtMost(requested)
        if (amount <= 0.0001) return 0.0
        return if (EconomyUtil.turnBankAccountToUserWallet(account, user, amount)) amount else 0.0
    }

    private data class LedgerCleanupEntry(
        val label: String,
        val description: String,
        val global: Boolean,
        val accountId: (String) -> String,
    )

    private val cleanupLedgerEntries = listOf(
        LedgerCleanupEntry("reserve", PrivateBankLedger.RESERVE_DESC, global = false) {
            PrivateBankLedger.accountId(it, PrivateBankLedger.RESERVE_DESC)
        },
        LedgerCleanupEntry("liquidity", PrivateBankLedger.LIQUIDITY_DESC, global = false) {
            PrivateBankLedger.accountId(it, PrivateBankLedger.LIQUIDITY_DESC)
        },
        LedgerCleanupEntry("loan inventory", PrivateBankLedger.INVENTORY_DESC, global = false) {
            PrivateBankLedger.accountId(it, PrivateBankLedger.INVENTORY_DESC)
        },
        LedgerCleanupEntry("legacy reserve", PrivateBankLedger.RESERVE_DESC, global = true) { it },
        LedgerCleanupEntry("legacy custom", PrivateBankLedger.LIQUIDITY_DESC, global = false) { it },
    )

    private fun hasAnyLedgerBalance(code: String): Boolean {
        return cleanupLedgerEntries.any { entry ->
            val accountId = entry.accountId(code)
            if (entry.global) {
                EconomyUtil.getMoneyByBankFromId(accountId, entry.description) > 0.0001
            } else {
                EconomyUtil.getMoneyFromPluginBankForId(accountId, entry.description) > 0.0001
            }
        }
    }

    private fun historicalReferenceCounts(code: String): List<Pair<String, Int>> =
        listOf(
            "储户存款" to PrivateBankRepository.listDeposits(code).size,
            "银行评价" to PrivateBankRepository.listReviews(code).size,
            "放贷额度" to PrivateBankRepository.listLoanOffers(code).size,
            "借款单" to PrivateBankRepository.listLoansByBank(code).size,
            "主银行债务" to if (PrivateBankRepository.findMainBankDebt(code) != null) 1 else 0,
            "国卷持仓" to PrivateBankRepository.listBondHoldings(code).size,
            "狐卷持仓" to PrivateBankRepository.listFoxBondHoldings(code).size,
            "狐卷竞标" to PrivateBankRepository.listAllFoxBondBids().count { it.bankCode == code },
            "狐卷中标" to PrivateBankRepository.listFoxBonds().count { it.winnerBankCode == code },
        ).filter { it.second > 0 }

    private fun hasAnyHistoricalPrivateBankData(code: String): Boolean =
        hasAnyLedgerBalance(code) || historicalReferenceCounts(code).isNotEmpty()

    fun clearOrphanPrivateBankLedger(codeRaw: String): Pair<Boolean, String> {
        val code = codeRaw.trim()
        if (!validateBankCode(code)) {
            return false to "清理失败：code 只能包含字母、数字、-、_，长度 3-48"
        }
        if (PrivateBankRepository.findBankByCode(code) != null) {
            return false to "清理失败：code=$code 仍有私人银行实体，不能当作孤儿账本清理"
        }

        val references = historicalReferenceCounts(code)
        if (references.isNotEmpty()) {
            val summary = references.joinToString("，") { "${it.first}${it.second}条" }
            return false to "清理失败：code=$code 仍有关联业务数据（$summary），请先迁移或改用新 code"
        }

        val cleared = mutableListOf<Pair<LedgerCleanupEntry, Double>>()
        for (entry in cleanupLedgerEntries) {
            val accountId = entry.accountId(code)
            val amount = if (entry.global) {
                EconomyUtil.getMoneyByBankFromId(accountId, entry.description)
            } else {
                EconomyUtil.getMoneyFromPluginBankForId(accountId, entry.description)
            }
            if (abs(amount) <= 0.0001) continue

            val ok = if (entry.global) {
                EconomyUtil.plusMoneyToBankFromId(accountId, entry.description, -amount)
            } else {
                EconomyUtil.plusMoneyToPluginBankForId(accountId, entry.description, -amount)
            }
            if (!ok) {
                cleared.forEach { (clearedEntry, clearedAmount) ->
                    val clearedAccountId = clearedEntry.accountId(code)
                    if (clearedEntry.global) {
                        EconomyUtil.plusMoneyToBankFromId(clearedAccountId, clearedEntry.description, clearedAmount)
                    } else {
                        EconomyUtil.plusMoneyToPluginBankForId(clearedAccountId, clearedEntry.description, clearedAmount)
                    }
                }
                return false to "清理失败：${entry.label} 扣减失败，已尝试回滚"
            }
            cleared += entry to amount
        }

        if (cleared.isEmpty()) {
            return true to "code=$code 没有可清理的孤儿私人银行账本"
        }

        val detail = cleared.joinToString("，") { (entry, amount) ->
            "${entry.label}${MoneyFormatUtil.format(amount)}"
        }
        return true to "已清理 code=$code 的孤儿私人银行账本：$detail"
    }

    private fun migrateBankCodeReferences(oldCode: String, newCode: String) {
        listOf(
            PrivateBankLedger.RESERVE_DESC,
            PrivateBankLedger.LIQUIDITY_DESC,
            PrivateBankLedger.INVENTORY_DESC
        ).forEach { migrateLedger(oldCode, newCode, it) }

        PrivateBankRepository.listDeposits(oldCode).forEach {
            it.bankCode = newCode
            PrivateBankRepository.saveDeposit(it)
        }
        PrivateBankRepository.listReviews(oldCode).forEach {
            it.bankCode = newCode
            PrivateBankRepository.addReview(it)
        }
        PrivateBankRepository.listLoanOffers(oldCode).forEach {
            it.bankCode = newCode
            PrivateBankRepository.saveLoanOffer(it)
        }
        PrivateBankRepository.listLoansByBank(oldCode).forEach {
            it.bankCode = newCode
            PrivateBankRepository.saveLoan(it)
        }
        PrivateBankRepository.findMainBankDebt(oldCode)?.let {
            it.bankCode = newCode
            PrivateBankRepository.saveMainBankDebt(it)
        }
        PrivateBankRepository.listBondHoldings(oldCode).forEach {
            it.bankCode = newCode
            PrivateBankRepository.saveBondHolding(it)
        }
        PrivateBankRepository.listFoxBondHoldings(oldCode).forEach {
            it.bankCode = newCode
            PrivateBankRepository.saveFoxBondHolding(it)
        }
        PrivateBankRepository.listAllFoxBondBids()
            .filter { it.bankCode == oldCode }
            .forEach {
                it.bankCode = newCode
                PrivateBankRepository.saveFoxBondBid(it)
            }
        PrivateBankRepository.listFoxBonds()
            .filter { it.winnerBankCode == oldCode }
            .forEach {
                it.winnerBankCode = newCode
                PrivateBankRepository.saveFoxBond(it)
            }

        updateDefaultBankCodes(oldCode, newCode)
    }

    private fun migrateLedger(oldCode: String, newCode: String, description: String) {
        val oldAccountId = PrivateBankLedger.accountId(oldCode, description)
        val newAccountId = PrivateBankLedger.accountId(newCode, description)
        val amount = EconomyUtil.getMoneyFromPluginBankForId(oldAccountId, description)
        if (amount <= 0.0001) return

        val debitOk = EconomyUtil.plusMoneyToPluginBankForId(oldAccountId, description, -amount)
        if (!debitOk) error("迁移旧账本失败: $oldCode/$description")

        val creditOk = EconomyUtil.plusMoneyToPluginBankForId(newAccountId, description, amount)
        if (!creditOk) {
            EconomyUtil.plusMoneyToPluginBankForId(oldAccountId, description, amount)
            error("迁移新账本失败: $newCode/$description")
        }
    }

    private fun updateDefaultBankCodes(oldCode: String, newCode: String) {
        val userProxy = EntityProxyRegistry.get<UserInfoDto>("user") ?: return
        userProxy.findWhere { it.defaultPrivateBankCode == oldCode }
            .forEach {
                it.defaultPrivateBankCode = newCode
                userProxy.save(it)
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
        val existing = PrivateBankRepository.findLoanOffer(offerId) ?: return false to "未找到贷款额度：$offerId"
        if (findBankByCodeOrName(existing.bankCode) == null) return false to "对应银行已经破产"
        return PrivateBankLocks.withBankLock(existing.bankCode) {
            updateLoanOfferInterestLocked(owner, offerId, ratePercent)
        }
    }

    private fun updateLoanOfferInterestLocked(owner: User, offerId: Int, ratePercent: Double): Pair<Boolean, String> {
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
        val existing = PrivateBankRepository.findLoanOffer(offerId) ?: return false to "未找到贷款额度：$offerId"
        if (findBankByCodeOrName(existing.bankCode) == null) return false to "对应银行已经破产"
        return PrivateBankLocks.withBankLock(existing.bankCode) {
            cancelLoanOfferLocked(owner, offerId)
        }
    }

    private fun cancelLoanOfferLocked(owner: User, offerId: Int): Pair<Boolean, String> {
        val offer = PrivateBankRepository.findLoanOffer(offerId) ?: return false to "未找到贷款额度：$offerId"
        val bank = findBankByCodeOrName(offer.bankCode) ?: return false to "未找到对应银行：${offer.bankCode}"
        if (bank.ownerQq != owner.id) return false to "只有行长可以撤回贷款额度"
        if (!offer.enabled && offer.remaining <= 0.0001) return false to "该贷款额度已关闭"

        val moves = buildLoanOfferCancelMoves(offer.remaining)
        val refund = moves.firstOrNull { it.description == PrivateBankLedger.LIQUIDITY_DESC }?.amount ?: 0.0
        if (moves.isNotEmpty()) {
            val debit = moves.first { it.description == PrivateBankLedger.INVENTORY_DESC }
            val credit = moves.first { it.description == PrivateBankLedger.LIQUIDITY_DESC }
            if (!PrivateBankLedger.debit(offer.bankCode, debit.description, -debit.amount)) {
                return false to "撤贷失败：放贷库存扣减失败"
            }
            if (!PrivateBankLedger.add(offer.bankCode, credit.description, credit.amount)) {
                PrivateBankLedger.add(offer.bankCode, debit.description, -debit.amount)
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

        val beforeLiquidity = PrivateBankLedger.balance(bankCode, debit.description)
        if (!PrivateBankLedger.debit(bankCode, debit.description, -debit.amount)) {
            return false
        }
        val afterDebitLiquidity = PrivateBankLedger.balance(bankCode, debit.description)
        val expectedLiquidity = ShareUtils.rounding(beforeLiquidity + debit.amount)
        if (abs(afterDebitLiquidity - expectedLiquidity) > 0.1) {
            if (afterDebitLiquidity < beforeLiquidity - 0.1) {
                PrivateBankLedger.add(bankCode, debit.description, -debit.amount)
            }
            return false
        }

        if (!PrivateBankLedger.add(bankCode, credit.description, credit.amount)) {
            PrivateBankLedger.add(bankCode, debit.description, -debit.amount)
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
        if (!PrivateBankLedger.add(bankCode, credit.description, credit.amount)) {
            EconomyUtil.plusMoneyToUser(owner, total)
            return false
        }
        return true
    }

    fun refreshRating(bankCode: String) {
        val bank = PrivateBankRepository.findBankByCode(bankCode) ?: return
        if (bank.isBankrupt()) {
            bank.star = 1
            PrivateBankRepository.saveBank(bank)
            return
        }
        val deposits = PrivateBankRepository.listDeposits(bank.code)
        val totalDeposit = deposits.sumOf { it.principal }

        val successRate = if (bank.withdrawRequests <= 0) 1.0 else
            ((bank.withdrawRequests - bank.withdrawFailures).toDouble() / bank.withdrawRequests.toDouble()).coerceIn(0.0, 1.0)

        val reviews = PrivateBankRepository.listReviews(bank.code)
        val avgReview = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()
        bank.avgReview = avgReview

        bank.star = calculateStar(totalDeposit, successRate, avgReview, bank.isDefaulter())

        PrivateBankRepository.saveBank(bank)
    }

    fun calculateStar(totalDeposit: Double, successRate: Double, avgReview: Double, defaulter: Boolean): Int {
        val depositScore = if (totalDeposit <= 0) 0.0 else (ln(totalDeposit + 1) / ln(10.0) / 7.0).coerceIn(0.0, 1.0)
        val reviewScore = (avgReview / 5.0).coerceIn(0.0, 1.0)
        val score = 0.45 * depositScore + 0.35 * successRate.coerceIn(0.0, 1.0) + 0.20 * reviewScore
        val baseStar = (1 + (score * 4.0)).toInt().coerceIn(1, 5)
        return if (defaulter) (baseStar - 2).coerceAtLeast(1) else baseStar
    }

    internal fun markDefaulter(bank: PrivateBankDto, now: Date = Date()) {
        val until = DateUtil.offsetDay(now, 30).time
        bank.defaulterUntil = maxOf(bank.defaulterUntil, until)
        bank.depositorInterest = baseInterest()
        PrivateBankRepository.saveBank(bank)
    }

    fun ensureDailyBondIssues(now: Date = Date()): List<PrivateBankGovBondIssueDto> {
        val existing = listDailyBondIssues(now)
        if (existing.isNotEmpty()) return existing
        if (!isAfterGovBondPublishTime(now)) return emptyList()

        val count = weightedChoice(listOf(1 to 50, 2 to 25, 3 to 15, 4 to 7, 5 to 3))
        return createDailyBondIssues(now, count)
    }

    fun supplementDailyBondIssues(now: Date = Date()): List<PrivateBankGovBondIssueDto> {
        val count = weightedChoice(listOf(1 to 50, 2 to 25, 3 to 15, 4 to 7, 5 to 3))
        return createDailyBondIssues(now, count)
    }

    private fun createDailyBondIssues(now: Date, count: Int): List<PrivateBankGovBondIssueDto> {
        val dateKey = DateUtil.format(now, "yyyyMMdd")
        val prefix = "GB-$dateKey-"
        val nextSequence = PrivateBankRepository.listBondIssues()
            .map { it.bondCode() }
            .filter { it.startsWith(prefix) }
            .mapNotNull { it.removePrefix(prefix).toIntOrNull() }
            .maxOrNull()
            ?.plus(1)
            ?: 1
        val issues = (nextSequence until nextSequence + count).map { seq ->
            val code = "GB-$dateKey-${seq.toString().padStart(3, '0')}"
            val totalLimit = weightedGovBondLimit()
            PrivateBankGovBondIssueDto(
                weekKey = code,
                code = code,
                rateMultiplier = RandomUtil.randomDouble(1.0, 3.01),
                lockDays = RandomUtil.randomInt(1, 8),
                totalLimit = totalLimit,
                remaining = totalLimit,
                createdAt = now.time
            )
        }
        return issues.map(PrivateBankRepository::saveBondIssue)
    }

    fun listAvailableBondIssues(now: Date = Date()): List<PrivateBankGovBondIssueDto> =
        PrivateBankRepository.listBondIssues()
            .filter { isBondIssueAvailable(it, now) }
            .sortedBy { bondRedeemAt(it) }

    fun bondRedeemAt(issue: PrivateBankGovBondIssueDto): Long =
        issue.createdAt + issue.lockDays.coerceAtLeast(0) * DAY_MILLIS

    fun isBondIssueAvailable(issue: PrivateBankGovBondIssueDto, now: Date = Date()): Boolean =
        issue.remaining > 0.0001 && bondRedeemAt(issue) - now.time > BOND_EXPIRING_WINDOW_MILLIS

    fun listDailyBondIssues(now: Date = Date()): List<PrivateBankGovBondIssueDto> {
        val prefix = "GB-${DateUtil.format(now, "yyyyMMdd")}-"
        return PrivateBankRepository.listBondIssues()
            .filter { it.bondCode().startsWith(prefix) }
            .sortedBy { it.bondCode() }
    }

    fun findBondIssueByCode(code: String): PrivateBankGovBondIssueDto? =
        PrivateBankRepository.findBondIssueByCode(code.trim())

    private fun PrivateBankGovBondIssueDto.bondCode(): String = code.ifBlank { weekKey }

    private fun isAfterGovBondPublishTime(now: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = now
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return hour > GOV_BOND_PUBLISH_HOUR || (hour == GOV_BOND_PUBLISH_HOUR && minute >= GOV_BOND_PUBLISH_MINUTE)
    }

    private fun weightedGovBondLimit(): Double {
        val million = weightedChoice(
            listOf(
                1 to 24,
                2 to 24,
                3 to 20,
                4 to 8,
                5 to 6,
                6 to 5,
                7 to 4,
                8 to 3,
                9 to 3,
                10 to 3
            )
        )
        return million * 1_000_000.0
    }

    private fun weightedChoice(weighted: List<Pair<Int, Int>>): Int {
        val totalWeight = weighted.sumOf { it.second.coerceAtLeast(0) }
        if (totalWeight <= 0) return weighted.firstOrNull()?.first ?: 0
        var point = RandomUtil.randomInt(1, totalWeight + 1)
        for ((value, weight) in weighted) {
            point -= weight.coerceAtLeast(0)
            if (point <= 0) return value
        }
        return weighted.last().first
    }

    fun buyBond(owner: User, bankCode: String, bondCode: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val bank = findBankByCodeOrName(bankCode) ?: return false to "未找到该银行或银行已经破产：$bankCode"
        if (bank.ownerQq != owner.id) return false to "只有行长可以购买国卷"

        val issue = findBondIssueByCode(bondCode) ?: return false to "未找到国卷：$bondCode"
        if (!isBondIssueAvailable(issue)) return false to "该国卷已过期或距离赎回不足24小时"
        if (issue.remaining < amount) return false to "该国卷剩余额度不足（剩余 ${MoneyFormatUtil.format(issue.remaining)}）"

        val liquidity = PrivateBankLedger.balance(bank.code, PrivateBankLedger.LIQUIDITY_DESC)
        if (liquidity < amount) return false to "流动金池余额不足（当前 ${MoneyFormatUtil.format(liquidity)}）"

        if (!PrivateBankLedger.debit(bank.code, PrivateBankLedger.LIQUIDITY_DESC, amount)) {
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

        return true to "购买成功：国卷 ${issue.bondCode()} ${MoneyFormatUtil.format(amount)}（锁仓 ${issue.lockDays} 天，利率 ${FormatUtil.fixed(issue.rateMultiplier, 2)}%/day）"
    }

    fun isBondMatured(holding: PrivateBankGovBondHoldingDto, now: Date = Date()): Boolean {
        val days = DateUtil.between(Date(holding.boughtAt), now, DateUnit.DAY)
        return days >= holding.lockDays
    }

    fun redeemBond(owner: User, bondCode: String, requestedAmount: Double? = null): Pair<Boolean, String> {
        val issue = findBondIssueByCode(bondCode) ?: return false to "未找到国卷：$bondCode"
        val bank = findOwnedActiveBank(owner.id) ?: return false to "你的银行不存在或已经破产"
        if (bank.ownerQq != owner.id) return false to "只有行长可以赎回国卷"

        val holdings = PrivateBankRepository.listBondHoldings(bank.code)
            .filter { it.issueId == issue.id && it.redeemedAt == 0L && it.principal > 0.0001 }
            .sortedBy { it.boughtAt }
        if (holdings.isEmpty()) return false to "你没有该国卷的可赎回持仓"

        val totalPrincipal = holdings.sumOf { it.principal }
        val amount = requestedAmount ?: totalPrincipal
        if (amount <= 0) return false to "赎回金额必须大于 0"
        if (amount > totalPrincipal + 1e-6) {
            return false to "该国卷持仓不足（当前 ${MoneyFormatUtil.format(totalPrincipal)}）"
        }

        var remaining = amount
        var payout = 0.0
        val now = Date()
        for (holding in holdings) {
            if (remaining <= 0.0001) break
            val principal = holding.principal.coerceAtMost(remaining)
            payout += calculateGovBondPayout(holding, principal, now)

            holding.principal = ShareUtils.rounding(holding.principal - principal)
            if (holding.principal <= 0.0001) {
                holding.principal = 0.0
                holding.redeemedAt = now.time
            }
            PrivateBankRepository.saveBondHolding(holding)
            remaining = ShareUtils.rounding(remaining - principal)
        }

        val roundedPayout = ShareUtils.rounding(payout)
        PrivateBankLedger.add(bank.code, PrivateBankLedger.LIQUIDITY_DESC, roundedPayout)

        return true to "赎回成功：国卷 ${issue.bondCode()} 本金=${MoneyFormatUtil.format(amount)} 回款=${MoneyFormatUtil.format(roundedPayout)}"
    }

    fun redeemBond(owner: User, holdingId: Int): Pair<Boolean, String> {
        val holding = PrivateBankRepository.findBondHolding(holdingId) ?: return false to "未找到该持仓"
        val issue = PrivateBankRepository.listBondIssues().firstOrNull { it.id == holding.issueId }
            ?: return false to "未找到该国卷"
        return redeemBond(owner, issue.bondCode(), holding.principal)
    }

    fun redeemMaturedBondHoldings(now: Date = Date()): Int {
        var redeemed = 0
        val holdings = PrivateBankRepository.listBanks().flatMap { bank ->
            PrivateBankRepository.listBondHoldings(bank.code)
                .filter { it.redeemedAt == 0L && it.principal > 0.0001 && isBondMatured(it, now) }
                .map { bank to it }
        }

        for ((bank, holding) in holdings) {
            val payout = calculateGovBondPayout(holding, holding.principal, now)
            if (PrivateBankLedger.add(bank.code, PrivateBankLedger.LIQUIDITY_DESC, payout)) {
                holding.principal = 0.0
                holding.redeemedAt = now.time
                PrivateBankRepository.saveBondHolding(holding)
                redeemed++
            }
        }
        return redeemed
    }

    private fun calculateGovBondPayout(
        holding: PrivateBankGovBondHoldingDto,
        principal: Double,
        now: Date = Date()
    ): Double {
        return if (isBondMatured(holding, now)) {
            ShareUtils.rounding(principal * (1 + (holding.rateMultiplier / 100.0) * holding.lockDays))
        } else {
            // 未到期提前赎回扣 10%。
            ShareUtils.rounding(principal * 0.9)
        }
    }

    fun publishLoan(owner: User, bankCode: String, total: Double, interest: Int, termDays: Int, source: String): Pair<Boolean, String> {
        val existing = findBankByCodeOrName(bankCode) ?: return false to "未找到该银行：$bankCode"
        return PrivateBankLocks.withBankLock(existing.code) {
            publishLoanLocked(owner, existing.code, total, interest, termDays, source)
        }
    }

    private fun publishLoanLocked(owner: User, bankCode: String, total: Double, interest: Int, termDays: Int, source: String): Pair<Boolean, String> {
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
            val liquidity = PrivateBankLedger.balance(bank.code, PrivateBankLedger.LIQUIDITY_DESC)
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
        return true to "新增一笔放贷,额度=${MoneyFormatUtil.format(savedOffer.remaining)},利率=${loanInterestPercentText(savedOffer.interest)}"
    }

    fun borrow(user: User, offerId: Int, amount: Double): Pair<Boolean, String> {
        val existing = PrivateBankRepository.findLoanOffer(offerId) ?: return false to "未找到贷款额度"
        if (findBankByCodeOrName(existing.bankCode) == null) return false to "对应银行已经破产"
        return PrivateBankLocks.withBankLock(existing.bankCode) {
            borrowLocked(user, offerId, amount)
        }
    }

    private fun borrowLocked(user: User, offerId: Int, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        validateBorrowerEligibility(user)?.let { return false to it }
        val offer = PrivateBankRepository.findLoanOffer(offerId) ?: return false to "未找到贷款额度"
        if (!offer.enabled) return false to "该贷款额度已关闭"
        if (offer.remaining < amount) return false to "剩余额度不足（剩余 ${MoneyFormatUtil.format(offer.remaining)}）"

        // 从放贷库存池扣除放款金额。
        if (!PrivateBankLedger.debit(offer.bankCode, PrivateBankLedger.INVENTORY_DESC, amount)) {
            return false to "放款失败：放贷库存扣减失败"
        }

        // 借款会进入用户在该银行的存款本金，实际资金进入银行流动金池。
        if (!PrivateBankLedger.add(offer.bankCode, PrivateBankLedger.LIQUIDITY_DESC, amount)) {
            PrivateBankLedger.add(offer.bankCode, PrivateBankLedger.INVENTORY_DESC, amount)
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
        val dueTotal = calculateLoanDueTotal(amount, offer.interest)
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

        return true to "借款成功: 本金=${MoneyFormatUtil.format(amount)}, 应还=${MoneyFormatUtil.format(dueTotal)}, 利息=${MoneyFormatUtil.format(interestAmount)}, 利率=${loanInterestPercentText(offer.interest)}, 最迟还款=${DateUtil.formatDateTime(dueAt)}"
    }

    fun repay(user: User, loanId: Int): Pair<Boolean, String> {
        val loan = PrivateBankRepository.findLoan(loanId) ?: return false to "未找到借款单"
        if (loan.borrowerQq != user.id) return false to "这不是你的借款单"
        if (loan.repaidAt != 0L) return false to "该借款已结清"
        if (normalizeLoanDueTotal(loan)) {
            PrivateBankRepository.saveLoan(loan)
        }
        val outstanding = ShareUtils.rounding((loan.dueTotal - loan.repaidAmount).coerceAtLeast(0.0))
        return repayByAmount(user, loanId, outstanding)
    }

    fun repayByAmount(user: User, loanId: Int, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "金额必须大于 0"
        val loan = PrivateBankRepository.findLoan(loanId) ?: return false to "未找到借款单"
        if (loan.borrowerQq != user.id) return false to "这不是你的借款单"
        if (loan.repaidAt != 0L) return false to "该借款已结清"
        if (normalizeLoanDueTotal(loan)) {
            PrivateBankRepository.saveLoan(loan)
        }

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

        PrivateBankLedger.add(loan.bankCode, PrivateBankLedger.LIQUIDITY_DESC, paid)
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
            if (normalizeLoanDueTotal(loan)) {
                PrivateBankRepository.saveLoan(loan)
            }
            if (loan.repaidAt != 0L) continue
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
            if (normalizeLoanDueTotal(loan, now.time)) {
                PrivateBankRepository.saveLoan(loan)
            }
            if (loan.repaidAt != 0L) continue
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

            PrivateBankLedger.add(loan.bankCode, PrivateBankLedger.LIQUIDITY_DESC, paid)
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
