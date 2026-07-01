package cn.chahuyun.economy.privatebank

import cn.chahuyun.economy.manager.BankManager
import cn.chahuyun.economy.model.privatebank.*
import cn.chahuyun.economy.utils.*
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.User
import java.util.*
import kotlin.math.ln

object PrivateBankService {

    /** 鍒涘缓闂ㄦ锛堜富閾惰浣欓锛?*/
    const val CREATE_THRESHOLD = 100_000_000.0

    /** 鍒涘缓鏃舵墸闄ゅ苟娌夋穩鍒扮閾惰祫浜х殑鍚姩璧勯噾 */
    const val CREATE_STARTUP_AMOUNT = 100_000_000.0

    private const val RESERVE_RATIO = 0.8
    private const val DEFAULT_STAR = 3
    private const val LOAN_TERM_DAYS = 14

    private fun baseInterest(): Int {
        return BankManager.getBankInfo(1)?.interest ?: 0
    }

    /** 涓婚摱琛屽熀鍑嗗埄鐜囷紙鐧惧垎姣斿€硷紝渚嬪 0.8 琛ㄧず 0.8%锛?*/
    fun baseInterestPercent(): Double = baseInterest() / 10.0

    private fun allowedInterestRange(base: Int): IntRange {
        // 卤0.3% -> interest 閲忕翰涓?0.1%锛屽洜姝?卤3
        return (base - 3)..(base + 3)
    }

    private fun findBankByCodeOrName(key: String): PrivateBankDto? {
        val trimmed = key.trim()
        if (trimmed.isBlank()) return null
        return PrivateBankRepository.findBankByCode(trimmed)
            ?: PrivateBankRepository.listBanks().firstOrNull { it.name == trimmed }
    }

    fun getBank(key: String): PrivateBankDto? = findBankByCodeOrName(key)

    fun borrowFromBank(user: User, bankKey: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "閲戦蹇呴』澶т簬 0"
        val bank = findBankByCodeOrName(bankKey) ?: return false to "鏈壘鍒拌閾惰锛?bankKey"
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
            return false to "创建失败：你已拥有自己的银行(code=${existingOwnerBank.code})"
        }

        val bankBalance = EconomyUtil.getMoneyByBank(owner)
        if (bankBalance < CREATE_THRESHOLD) {
            return false to "创建失败：主银行余额需达到 100,000,000（当前 ${MoneyFormatUtil.format(bankBalance)}）"
        }

        val code = (codeInput ?: "").trim().ifBlank { generateBankCode(owner.id) }
        if (!validateBankCode(code)) {
            return false to "鍒涘缓澶辫触锛氶摱琛?code 浠呭厑璁稿瓧姣嶆暟瀛楀強 -_锛岄暱搴?3-48"
        }
        if (PrivateBankRepository.findBankByCode(code) != null) {
            return false to "鍒涘缓澶辫触锛氳 code 宸茶鍗犵敤锛?code"
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

        // 1) 鍚姩璧勯噾 80%锛氫粠琛岄暱涓婚摱琛?-> 绉侀摱鍦ㄤ富閾惰鐨勫噯澶囬噾瀛愯处鎴?
        if (!EconomyUtil.turnUserGlobalBankToGlobalBankAccount(owner, bank.code, PrivateBankLedger.RESERVE_DESC, reservePart)) {
            return false to "创建失败：启动资金划转准备金失败"
        }

        // 2) 鍚姩璧勯噾 20%锛氫粠琛岄暱涓婚摱琛?-> 绉侀摱鑷畾涔夎处鏈殑娴佸姩閲戞睜
        if (!EconomyUtil.turnUserGlobalBankToPluginBankForId(owner, bank.code, PrivateBankLedger.LIQUIDITY_DESC, liquidityPart)) {
            // 鍥炴粴鍑嗗閲?
            EconomyUtil.plusMoneyToBankFromId(bank.code, PrivateBankLedger.RESERVE_DESC, -reservePart)
            EconomyUtil.plusMoneyToBank(owner, reservePart)
            return false to "鍒涘缓澶辫触锛氬惎鍔ㄨ祫閲戝垝杞?娴佸姩閲?澶辫触"
        }

        PrivateBankRepository.saveBank(bank)
        return true to "创建成功：${bank.name}(code=${bank.code}) 启动资金 100,000,000 已入账（准备金 ${MoneyFormatUtil.format(reservePart)} / 流动金 ${MoneyFormatUtil.format(liquidityPart)}）"
    }

    fun deposit(user: User, bankCode: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "閲戦蹇呴』澶т簬 0"

        val bank = findBankByCodeOrName(bankCode) ?: return false to "鏈壘鍒拌閾惰锛?bankCode"
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

        // 1) reservePart锛氶挶鍖?custom) -> 涓婚摱琛?global) bankCode/pb-reserve
        if (!EconomyUtil.turnUserWalletToGlobalBankAccount(user, bank.code, PrivateBankLedger.RESERVE_DESC, reservePart)) {
            return false to "瀛樺叆澶辫触锛氬噯澶囬噾鍒掕浆澶辫触"
        }

        // 2) liquidityPart锛氶挶鍖呮墸鍑?+ 杩涘叆娴佸姩閲戞睜(custom)
        if (!EconomyUtil.minusMoneyToUser(user, liquidityPart)) {
            // 鍥炴粴鍑嗗閲?
            EconomyUtil.turnGlobalBankAccountToUserWallet(bank.code, PrivateBankLedger.RESERVE_DESC, user, reservePart)
            return false to "存入失败：钱包扣减失败"
        }
        if (!EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC, liquidityPart)) {
            // 鍥炴粴
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

        return true to "存入成功：${MoneyFormatUtil.format(amount)}（准备金 ${MoneyFormatUtil.format(reservePart)} / 流动金 ${MoneyFormatUtil.format(liquidityPart)}）"
    }

    fun withdraw(user: User, bankCode: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "閲戦蹇呴』澶т簬 0"
        val bank = findBankByCodeOrName(bankCode) ?: return false to "鏈壘鍒拌閾惰锛?bankCode"

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
            true to "鍙栨鎴愬姛锛?{MoneyFormatUtil.format(amount)}"
        } else {
            bank.withdrawFailures += 1
            bank.defaulterUntil = DateUtil.offsetDay(Date(), 30).time
            bank.depositorInterest = baseInterest() // 澶变俊鏈熼棿寮哄埗鍚屾涓婚摱琛屽埄鐜?
            PrivateBankRepository.saveBank(bank)
            refreshRating(bank.code)
            false to "鍙栨澶辫触锛氳閾惰璧勯噾閾句笉瓒筹紝宸叉爣璁颁负澶变俊閾惰锛?0澶╋級"
        }
    }

    private fun forcePayToUserWallet(bank: PrivateBankDto, amount: Double, user: User): Boolean {
        var remaining = amount

        // 1) 娴佸姩閲戞睜(custom)
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

        // 2) 鍑嗗閲戞睜(global)
        val reserve = EconomyUtil.getMoneyByBankFromId(bank.code, PrivateBankLedger.RESERVE_DESC)
        val takeReserve = reserve.coerceAtMost(remaining)
        if (takeReserve > 0) {
            if (!EconomyUtil.turnGlobalBankAccountToUserWallet(bank.code, PrivateBankLedger.RESERVE_DESC, user, takeReserve)) return false
            remaining -= takeReserve
        }

        if (remaining <= 0) return true

        // 3) 閾惰搴撳瓨(custom)
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

        // 4) 琛岄暱涓汉閽卞寘(custom)
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

        // 5) 鍥藉嵎璧勪骇锛堝己鍒舵彁鍓嶆姌浠疯祹鍥烇級
        remaining = forceLiquidateBonds(bank, remaining, user) ?: return false
        if (remaining <= 0) return true

        // 6) 椋庨櫓淇濊瘉閲?custom)
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
     * 寮哄埗鎻愬墠璧庡洖鍥藉嵎锛堟姌浠?10%锛夛紝鎶婅祫閲戠洿鎺ユ敮浠樼粰鍙栨鐢ㄦ埛銆?
     * 杩斿洖鍓╀綑鏈鐩栭噾棰濓紱杩斿洖 null 琛ㄧず澶勭悊寮傚父銆?
     */
    private fun forceLiquidateBonds(bank: PrivateBankDto, remaining: Double, user: User): Double? {
        var left = remaining
        return try {
            val holdings = PrivateBankRepository.listBondHoldings(bank.code).filter { it.redeemedAt == 0L }
                .sortedBy { it.boughtAt }

            for (h in holdings) {
                if (left <= 0) break
                val redeemable = (h.principal * 0.9).coerceAtMost(left) // 鎶樹环
                if (redeemable <= 0) continue

                // 鎸夋瘮渚嬬缉鍑忔寔浠?
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
            Log.error("閾惰:寮哄埗璧庡洖鍥藉嵎澶辫触", e)
            null
        }
    }

    fun addReview(user: User, bankCode: String, rating: Int): Pair<Boolean, String> {
        return addReview(user, bankCode, rating, null)
    }

    fun addReview(user: User, bankCode: String, rating: Int, content: String?): Pair<Boolean, String> {
        if (rating !in 1..5) return false to "璇勫垎蹇呴』鍦?1-5"
        val bank = findBankByCodeOrName(bankCode) ?: return false to "鏈壘鍒拌閾惰锛?bankCode"

        val deposit = PrivateBankRepository.findDeposit(bank.code, user.id)
            ?: return false to "璇勫垎澶辫触锛氫綘鍦ㄨ閾惰娌℃湁瀛樻"

        val days = DateUtil.between(Date(deposit.createdAt), Date(), DateUnit.DAY)
        if (days < 7) {
            return false to "璇勫垎澶辫触锛氬瓨娆鹃渶婊?7 澶╁悗鎵嶅彲璇勫垎锛堝綋鍓?${days} 澶╋級"
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
        val bank = findBankByCodeOrName(bankCode) ?: return false to "鏈壘鍒拌閾惰锛?bankCode"
        if (bank.ownerQq != owner.id) return false to "鍙湁琛岄暱鍙互璁剧疆鍒╃巼"
        if (bank.isDefaulter()) return false to "澶变俊鏈熼棿绂佹璋冩暣鍒╃巼"

        val base = baseInterest()
        val allowed = allowedInterestRange(base)

        val nextInterest = when (val key = modeOrRate.trim().lowercase(Locale.getDefault())) {
            "max" -> allowed.last
            "min" -> allowed.first
            "now" -> base
            else -> {
                // 鏀寔杈撳叆 1.2锛堢櫨鍒嗘瘮锛夋垨 12锛坕nterest 鏁存暟锛?
                val v = key.toDoubleOrNull() ?: return false to "鍒╃巼鍙傛暟閿欒锛氳濉?rate/max/min/now"
                val asInterest = if (v < 10) {
                    // 瑙嗕负鐧惧垎姣旓紝渚嬪 1.2 -> 12
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
        return true to "璁剧疆鎴愬姛锛氬綋鍓嶅埄鐜?${FormatUtil.fixed(bank.depositorInterest / 10.0, 1)}%"
    }

    private fun maxLoanOffersByStar(star: Int): Int = star.coerceIn(1, 5)

    private fun maxLoanInterestByStar(star: Int): Int {
        // interest 閲忕翰涓?0.1%锛岃繖閲岀粰涓€涓殢鏄熺骇鎻愬崌鐨勪笂闄?
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

        // 绠€鍗曡瘎鍒嗘ā鍨嬶細瀛樻瑙勬ā(瀵规暟) + 鍙栨鎴愬姛鐜?+ 璇勪环
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
        if (amount <= 0) return false to "閲戦蹇呴』澶т簬 0"
        val bank = PrivateBankRepository.findBankByCode(bankCode) ?: return false to "鏈壘鍒拌閾惰锛?bankCode"
        if (bank.ownerQq != owner.id) return false to "鍙湁琛岄暱鍙互璐拱鍥藉嵎"

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

    fun redeemBond(owner: User, holdingId: Int): Pair<Boolean, String> {
        val holding = PrivateBankRepository.findBondHolding(holdingId) ?: return false to "鏈壘鍒拌鎸佷粨"
        val bank = PrivateBankRepository.findBankByCode(holding.bankCode) ?: return false to "未找到对应银行"
        if (bank.ownerQq != owner.id) return false to "鍙湁琛岄暱鍙互璧庡洖鍥藉嵎"
        if (holding.redeemedAt != 0L) return false to "璇ユ寔浠撳凡璧庡洖"

        val days = DateUtil.between(Date(holding.boughtAt), Date(), DateUnit.DAY)
        val base = baseInterest()

        val payout = if (days >= holding.lockDays) {
            // 鍒版湡锛氭寜涓婚摱琛屽埄鐜?* 鍊嶆暟璁＄畻涓€娆℃€ф敹鐩婏紙绠€鍖栵細鎸夊ぉ璁℃伅锛?
            val rate = (base / 1000.0) * holding.rateMultiplier
            ShareUtils.rounding(holding.principal * (1 + rate * holding.lockDays))
        } else {
            // 鎻愬墠锛氭姌浠?10%
            ShareUtils.rounding(holding.principal * 0.9)
        }

        holding.redeemedAt = System.currentTimeMillis()
        PrivateBankRepository.saveBondHolding(holding)

        // 璧庡洖璧勯噾杩涘叆娴佸姩閲戞睜
        EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC, payout)

        return true to "璧庡洖鎴愬姛锛氬埌璐?${MoneyFormatUtil.format(payout)}"
    }

    fun publishLoan(owner: User, bankCode: String, total: Double, interest: Int, termDays: Int, source: String): Pair<Boolean, String> {
        if (total <= 0) return false to "閲戦蹇呴』澶т簬 0"
        if (termDays !in 1..30) return false to "期限仅支持 1-30 天"

        val bank = findBankByCodeOrName(bankCode) ?: return false to "鏈壘鍒拌閾惰锛?bankCode"
        if (bank.ownerQq != owner.id) return false to "鍙湁琛岄暱鍙互鍙戝竷璐锋"

        val activeOffers = PrivateBankRepository.listLoanOffers(bank.code)
            .count { it.enabled && it.remaining > 0.0001 }
        if (activeOffers >= maxLoanOffersByStar(bank.star)) {
            return false to "发布失败：当前银行星级最多允许 ${maxLoanOffersByStar(bank.star)} 笔放贷标的"
        }

        if (interest <= 0 || interest > maxLoanInterestByStar(bank.star)) {
            return false to "发布失败：利息超出星级限制（星级 ${bank.star} 最大允许 ${FormatUtil.fixed(maxLoanInterestByStar(bank.star) / 10.0, 1)}%）"
        }

        val src = source.uppercase(Locale.getDefault())
        if (src != "LIQUIDITY" && src != "OWNER") return false to "source 浠呮敮鎸?LIQUIDITY/OWNER"

        // 鍏堟妸璧勯噾鍐荤粨鍒板簱瀛樿处鎴凤紝閬垮厤鍚庣画鍊熸鏃惰祫閲戜笉瓒?
        val ok = if (src == "LIQUIDITY") {
            val liquidity = EconomyUtil.getMoneyFromPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC)
            if (liquidity < total) return false to "娴佸姩閲戞睜浣欓涓嶈冻"
            EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.LIQUIDITY_DESC, -total) &&
                EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.INVENTORY_DESC, total)
        } else {
            val ownerWallet = EconomyUtil.getMoneyByUser(owner)
            if (ownerWallet < total) return false to "琛岄暱閽卞寘浣欓涓嶈冻"
            EconomyUtil.minusMoneyToUser(owner, total) &&
                EconomyUtil.plusMoneyToPluginBankForId(bank.code, PrivateBankLedger.INVENTORY_DESC, total)
        }

        if (!ok) return false to "发布失败：资金冻结失败"

        val offer = PrivateBankLoanOfferDto(
            bankCode = bank.code,
            ownerQq = owner.id,
            source = src,
            total = total,
            remaining = total,
            interest = interest,
            termDays = termDays
        )
        val savedOffer = PrivateBankRepository.saveLoanOffer(offer)
        return true to "鍙戝竷鎴愬姛锛歰fferId=${savedOffer.id} 鍙€?${MoneyFormatUtil.format(savedOffer.remaining)}"
    }

    fun borrow(user: User, offerId: Int, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "閲戦蹇呴』澶т簬 0"
        val offer = PrivateBankRepository.findLoanOffer(offerId) ?: return false to "鏈壘鍒拌璐锋鏍囩殑"
        if (!offer.enabled) return false to "璇ヨ捶娆炬爣鐨勫凡鍏抽棴"
        if (offer.remaining < amount) return false to "剩余额度不足（剩余 ${MoneyFormatUtil.format(offer.remaining)}）"

        // 璁″垝鍙ｅ緞锛氳捶娆鹃噾棰濊繘鍏ュ€熸浜哄湪璇ラ摱琛岀殑鈥滃瓨娆捐处鎴封€濓紙鐢ㄦ埛鍙嚜琛屽彇娆句娇鐢級
        if (!EconomyUtil.plusMoneyToPluginBankForId(offer.bankCode, PrivateBankLedger.INVENTORY_DESC, -amount)) {
            return false to "放款失败：放贷库存扣减失败"
        }

        // 鎶婅祫閲戣浆鍏ユ祦鍔ㄩ噾姹犱互瑕嗙洊鍙兘鐨勫彇娆撅紙璧勪骇褰㈡€佷粠鏀捐捶浠撳簱鍙樹负娴佸姩閲戯級
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
        val savedLoan = PrivateBankRepository.saveLoan(loan)

        return true to "鍊熸鎴愬姛锛歭oanId=${savedLoan.id} 閲戦=${MoneyFormatUtil.format(amount)} 搴旇繕=${MoneyFormatUtil.format(dueTotal)} 鍒版湡鏃?${DateUtil.formatDateTime(dueAt)}"
    }

    fun repay(user: User, loanId: Int): Pair<Boolean, String> {
        val loan = PrivateBankRepository.findLoan(loanId) ?: return false to "未找到借款单"
        if (loan.borrowerQq != user.id) return false to "鍙兘鍋胯繕鑷繁鐨勫€熸"
        if (loan.repaidAt != 0L) return false to "该借款已结清"
        val outstanding = ShareUtils.rounding((loan.dueTotal - loan.repaidAmount).coerceAtLeast(0.0))
        return repayByAmount(user, loanId, outstanding)
    }

    fun repayByAmount(user: User, loanId: Int, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "閲戦蹇呴』澶т簬 0"
        val loan = PrivateBankRepository.findLoan(loanId) ?: return false to "未找到借款单"
        if (loan.borrowerQq != user.id) return false to "鍙兘鍋胯繕鑷繁鐨勫€熸"
        if (loan.repaidAt != 0L) return false to "该借款已结清"

        val outstanding = (loan.dueTotal - loan.repaidAmount).coerceAtLeast(0.0)
        if (outstanding <= 0.0001) {
            loan.repaidAt = System.currentTimeMillis()
            PrivateBankRepository.saveLoan(loan)
            return true to "该借款已结清"
        }

        val pay = amount.coerceAtMost(outstanding)

        // 鎵ｆ锛氫紭鍏堥挶鍖咃紝涓嶈冻鍐嶆墸涓婚摱琛?
        var remaining = pay
        val wallet = EconomyUtil.getMoneyByUser(user)
        val takeWallet = wallet.coerceAtMost(remaining)
        if (takeWallet > 0) {
            if (!EconomyUtil.minusMoneyToUser(user, takeWallet)) return false to "鎵ｆ澶辫触"
            remaining -= takeWallet
        }
        if (remaining > 0) {
            val bank = EconomyUtil.getMoneyByBank(user)
            val takeBank = bank.coerceAtMost(remaining)
            if (takeBank > 0) {
                if (!EconomyUtil.plusMoneyToBank(user, -takeBank)) {
                    // 鍥炴粴閽卞寘鎵ｆ
                    if (takeWallet > 0) EconomyUtil.plusMoneyToUser(user, takeWallet)
                    return false to "鎵ｆ澶辫触"
                }
                remaining -= takeBank
            }
        }

        val paid = ShareUtils.rounding(pay - remaining)
        if (paid <= 0) return false to "浣欓涓嶈冻"

        EconomyUtil.plusMoneyToPluginBankForId(loan.bankCode, PrivateBankLedger.LIQUIDITY_DESC, paid)
        loan.repaidAmount = ShareUtils.rounding(loan.repaidAmount + paid)
        if (loan.repaidAmount + 0.0001 >= loan.dueTotal) {
            loan.repaidAt = System.currentTimeMillis()
        }
        PrivateBankRepository.saveLoan(loan)

        val left = ShareUtils.rounding((loan.dueTotal - loan.repaidAmount).coerceAtLeast(0.0))
        return true to "杩樻鎴愬姛锛氬凡杩?${MoneyFormatUtil.format(paid)}锛屽墿浣?${MoneyFormatUtil.format(left)}"
    }

    fun repayToBankByAmount(user: User, bankCode: String, amount: Double): Pair<Boolean, String> {
        if (amount <= 0) return false to "閲戦蹇呴』澶т簬 0"
        val bank = findBankByCodeOrName(bankCode) ?: return false to "鏈壘鍒拌閾惰锛?bankCode"
        val loans = PrivateBankRepository.listLoansByBorrower(user.id)
            .filter { it.repaidAt == 0L && it.bankCode == bank.code }
            .sortedBy { it.createdAt }

        if (loans.isEmpty()) return false to "浣犲湪璇ラ摱琛屾病鏈夋湭缁撴竻鍊熸"

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

        return true to "宸插皾璇曡繕娆撅細${MoneyFormatUtil.format(paidTotal)}"
    }

    /**
     * 鍒版湡鑷姩鎵ｆ锛氫紭鍏堜富閾惰锛屽叾娆￠挶鍖咃紱鎵ｅ埌鐨勯挶杩涘叆绉侀摱娴佸姩閲戞睜銆?
     * 鍙兘鍥犱负鐢ㄦ埛绂荤嚎/鏃犳硶瑙ｆ瀽 User 瀵硅薄鑰岃烦杩囥€?
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

            // 鑷姩鎵ｏ細浼樺厛涓婚摱琛?
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
