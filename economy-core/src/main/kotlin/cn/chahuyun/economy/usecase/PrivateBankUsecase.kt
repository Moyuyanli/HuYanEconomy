@file:Suppress("DuplicatedCode")

package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.data.repository.PrivateBankRepository
import cn.chahuyun.economy.image.PrivateBankInfoImageRenderer
import cn.chahuyun.economy.image.model.BankInfoFundLine
import cn.chahuyun.economy.image.model.BankInfoLoanLine
import cn.chahuyun.economy.image.model.PrivateBankInfoCard
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.privatebank.PrivateBankDto
import cn.chahuyun.economy.model.privatebank.PrivateBankLoanDto
import cn.chahuyun.economy.model.privatebank.PrivateBankLoanOfferDto
import cn.chahuyun.economy.privatebank.*
import cn.chahuyun.economy.utils.*
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import java.util.concurrent.ConcurrentHashMap

/**
 * 银行用例（代码层模块名仍为 PrivateBank；用户侧统一称为“银行”）。
 */
object PrivateBankUsecase {

    private const val LOAN_OFFER_DEDUP_WINDOW_MS = 5_000L
    private val recentLoanOfferCommands = ConcurrentHashMap<String, Long>()

    data class PrivateBankListItem(
        val bank: PrivateBankDto,
        val owner: String,
        val totalDeposit: Double,
        val remainingLoanLimit: Double,
        val outstandingLoan: Double,
        val inventory: Double,
        val withdrawSuccessRate: Double,
        val mainBankDebt: Double = 0.0,
        val defaulter: Boolean = false,
        val minInterestOffer: LoanOfferListItem? = null,
        val maxInterestOffer: LoanOfferListItem? = null,
    )

    data class LoanOfferListItem(
        val remaining: Double,
        val interest: Int,
        val id: Int = 0,
    )

    private fun displayUser(subject: Contact, qq: Long): String {
        val nick = (subject as? Group)
            ?.get(qq)
            ?.nameCardOrNick
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        return if (nick != null) "$nick($qq)" else qq.toString()
    }

    private fun resolveUser(subject: Contact, qq: Long): User? {
        (subject as? Group)?.get(qq)?.let { return it }
        for (bot in Bot.instances) {
            bot.getFriend(qq)?.let { return it }
            bot.getStranger(qq)?.let { return it }
            for (group in bot.groups) {
                group.get(qq)?.let { return it }
            }
        }
        return null
    }

    fun buildBankListItems(subject: Contact, banks: List<PrivateBankDto>): List<PrivateBankListItem> {
        return banks.map { bank ->
            val mainBankDebt = PrivateBankDebtService.outstanding(PrivateBankDebtService.accrue(bank.code))
            val totalDeposit = PrivateBankRepository.listDeposits(bank.code).sumOf { it.principal }
            val allLoans = PrivateBankRepository.listLoansByBank(bank.code)
            val visibleOffers = PrivateBankRepository.listLoanOffers(bank.code)
                .filter { offer -> isVisibleLoanOffer(offer, allLoans) }
            val visibleOfferIds = visibleOffers.mapTo(hashSetOf()) { it.id }
            val borrowableOffers = visibleOffers.filter { it.remaining > 0.0001 }
            val offersByInterest = borrowableOffers.sortedWith(
                compareBy<PrivateBankLoanOfferDto> { it.interest }
                    .thenBy { it.createdAt }
                    .thenBy { it.id }
            )
            val activeLoans = allLoans.filter { it.offerId in visibleOfferIds && isOutstandingLoan(it) }
            val inventory = PrivateBankLedger.balance(bank.code, PrivateBankLedger.INVENTORY_DESC)
            val borrowableAmount = calculateBorrowableAmount(borrowableOffers, inventory)
            val withdrawSuccessRate = if (bank.withdrawRequests <= 0) {
                100.0
            } else {
                100.0 * (bank.withdrawRequests - bank.withdrawFailures) / bank.withdrawRequests.toDouble()
            }

            PrivateBankListItem(
                bank = bank,
                owner = displayUser(subject, bank.ownerQq),
                totalDeposit = totalDeposit,
                remainingLoanLimit = borrowableAmount,
                outstandingLoan = activeLoans.sumOf { (it.dueTotal - it.repaidAmount).coerceAtLeast(0.0) },
                inventory = inventory,
                withdrawSuccessRate = withdrawSuccessRate,
                mainBankDebt = mainBankDebt,
                defaulter = (bank.defaulterUntil > System.currentTimeMillis()) || mainBankDebt > 0.0001,
                minInterestOffer = offersByInterest.firstOrNull()?.let {
                    LoanOfferListItem(it.remaining, it.interest, it.id)
                },
                maxInterestOffer = offersByInterest.lastOrNull()?.let {
                    LoanOfferListItem(it.remaining, it.interest, it.id)
                },
            )
        }.sortedWith(
            compareBy<PrivateBankListItem> { it.defaulter }
                .thenByDescending { it.bank.star }
                .thenByDescending { it.bank.avgReview }
                .thenByDescending { it.totalDeposit }
                .thenBy { it.bank.code }
        )
    }

    fun formatBankListItem(index: Int, item: PrivateBankListItem): String {
        val bank = item.bank
        val defaulterText = if (item.defaulter) {
            val until = bank.defaulterUntil.takeIf { it > System.currentTimeMillis() }
            when {
                until != null && item.mainBankDebt > 0.0001 ->
                    "是，至少到 ${DateUtil.formatDateTime(java.util.Date(until))} 且债务清零"
                until != null -> "是，到 ${DateUtil.formatDateTime(java.util.Date(until))}"
                item.mainBankDebt > 0.0001 -> "是，主银行债务清零后解除"
                else -> "是"
            }
        } else {
            "否"
        }

        return buildString {
            append("#").append(index).append("  ")
            append(bank.name).append("  code=").append(bank.code).append('\n')
            append("行长：").append(item.owner).append('\n')
            append("星级：").append("★".repeat(bank.star.coerceIn(1, 5)))
            append("（").append(bank.star.coerceIn(1, 5)).append("）")
            append(" / 评分：").append(FormatUtil.fixed(bank.avgReview, 2)).append('\n')
            append("存款利率：").append(FormatUtil.fixed(bank.depositorInterest / 10.0, 1)).append("%")
            append(" / 取款成功率：").append(FormatUtil.fixed(item.withdrawSuccessRate, 1)).append("%").append('\n')
            append("存款总额：").append(MoneyFormatUtil.format(item.totalDeposit))
            append(" / 可借额度：").append(MoneyFormatUtil.format(item.remainingLoanLimit)).append('\n')
            append("放贷库存：").append(MoneyFormatUtil.format(item.remainingLoanLimit)).append(" (可借贷)")
            append("/ ").append(MoneyFormatUtil.format(item.inventory)).append("(总库存) ")
            append("待收本息：").append(MoneyFormatUtil.format(item.outstandingLoan)).append('\n')
            append("放贷利息：").append(formatLoanOfferRange(item)).append('\n')
            append("失信：").append(defaulterText)
            if (item.mainBankDebt > 0.0001) {
                append(" / 主银行债务：").append(MoneyFormatUtil.format(item.mainBankDebt))
            }
            bank.slogan.trim().takeIf { it.isNotBlank() }?.let {
                append('\n').append("描述：").append(it.take(80))
            }
        }
    }

    internal fun formatLoanOfferRange(item: PrivateBankListItem): String {
        val minOffer = item.minInterestOffer ?: return "暂无"
        val maxOffer = item.maxInterestOffer
        val minText = formatLoanOffer(minOffer)
        return if (maxOffer == null || maxOffer == minOffer) {
            minText
        } else {
            "$minText / ${formatLoanOffer(maxOffer)}"
        }
    }

    private fun formatLoanOffer(offer: LoanOfferListItem): String =
        "${MoneyFormatUtil.format(offer.remaining)}(${formatInterestPercent(offer.interest)})"

    internal fun isOutstandingLoan(loan: PrivateBankLoanDto): Boolean =
        loan.repaidAt == 0L && (loan.dueTotal - loan.repaidAmount) > 0.0001

    internal fun isVisibleLoanOffer(offer: PrivateBankLoanOfferDto, loans: List<PrivateBankLoanDto>): Boolean =
        offer.enabled && (offer.remaining > 0.0001 || loans.any { it.offerId == offer.id && isOutstandingLoan(it) })

    internal fun calculateBorrowableAmount(offers: List<PrivateBankLoanOfferDto>, inventory: Double): Double =
        offers.asSequence()
            .filter { it.enabled && it.remaining > 0.0001 }
            .sumOf { it.remaining }
            .coerceAtMost(inventory.coerceAtLeast(0.0))

    internal fun formatLoanBorrower(loan: PrivateBankLoanDto, borrower: String): String {
        val interest = (loan.dueTotal - loan.principal).coerceAtLeast(0.0)
        return "- $borrower 借贷 额度=${MoneyFormatUtil.format(loan.principal)} " +
            "利息=${MoneyFormatUtil.format(interest)} 未还款"
    }

    fun markLoanOfferCommandIfFresh(key: String, now: Long = System.currentTimeMillis()): Boolean {
        recentLoanOfferCommands.entries.removeIf { now - it.value > LOAN_OFFER_DEDUP_WINDOW_MS }
        val previous = recentLoanOfferCommands.putIfAbsent(key, now) ?: return true
        return if (now - previous > LOAN_OFFER_DEDUP_WINDOW_MS) {
            recentLoanOfferCommands[key] = now
            true
        } else {
            false
        }
    }

    private suspend fun sendBankInfo(event: MessageEvent, bankKey: String) {
        val subject: Contact = event.subject
        val bank = PrivateBankService.getBank(bankKey)
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "未找到该银行：$bankKey"))
            return
        }
        val card = buildPrivateBankInfoCard(subject, bank)

        try {
            val image = PrivateBankInfoImageRenderer.render(card)
            ImageMessageUtil.sendQuotedImage(subject, event.message, image)
        } catch (e: Exception) {
            Log.error("银行信息图片生成或发送失败", e)
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    "银行信息生成失败：${bank.name}(code=${bank.code})"
                )
            )
        }
    }

    private suspend fun sendBankInfoText(event: MessageEvent, bank: PrivateBankDto) {
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, formatPrivateBankInfoCard(buildPrivateBankInfoCard(event.subject, bank))))
    }

    private fun buildPrivateBankInfoCard(subject: Contact, bank: PrivateBankDto): PrivateBankInfoCard {
        val deposits = PrivateBankRepository.listDeposits(bank.code)
        val totalDeposit = deposits.sumOf { it.principal }
        val reserve = PrivateBankLedger.balance(bank.code, PrivateBankLedger.RESERVE_DESC)
        val liquidity = PrivateBankLedger.balance(bank.code, PrivateBankLedger.LIQUIDITY_DESC)
        val inventory = PrivateBankLedger.balance(bank.code, PrivateBankLedger.INVENTORY_DESC)
        val ownerBankBalance = resolveUser(subject, bank.ownerQq)
            ?.let { EconomyUtil.getMoneyByBank(it) }
            ?: 0.0
        val govBondPrincipal = PrivateBankRepository.listBondHoldings(bank.code)
            .filter { it.redeemedAt == 0L }
            .sumOf { it.principal }
        val foxBondPrincipal = PrivateBankRepository.listFoxBondHoldings(bank.code)
            .filter { it.redeemedAt == 0L }
            .sumOf { it.principal }
        val allLoans = PrivateBankRepository.listLoansByBank(bank.code)
        val mainBankDebt = PrivateBankDebtService.accrue(bank.code)
        val activeOffers = PrivateBankRepository.listLoanOffers(bank.code)
            .filter { offer -> isVisibleLoanOffer(offer, allLoans) }
            .sortedByDescending { it.remaining }
        val activeOfferIds = activeOffers.mapTo(hashSetOf()) { it.id }
        val activeLoans = allLoans.filter { it.offerId in activeOfferIds && isOutstandingLoan(it) }
        val totalLoanLimit = activeOffers.sumOf { it.total }
        val remainingLoanLimit = calculateBorrowableAmount(activeOffers, inventory)
        val outstandingLoan = activeLoans.sumOf { (it.dueTotal - it.repaidAmount).coerceAtLeast(0.0) }

        val withdrawSuccessRate = if (bank.withdrawRequests <= 0) {
            100.0
        } else {
            100.0 * (bank.withdrawRequests - bank.withdrawFailures) / bank.withdrawRequests.toDouble()
        }

        return PrivateBankInfoCard(
            name = bank.name,
            code = bank.code,
            slogan = bank.slogan,
            owner = displayUser(subject, bank.ownerQq),
            star = bank.star,
            interest = "${FormatUtil.fixed(bank.depositorInterest / 10.0, 1)}%",
            avgReview = FormatUtil.fixed(bank.avgReview, 2),
            totalDeposit = MoneyFormatUtil.format(totalDeposit),
            withdrawSuccessRate = "${FormatUtil.fixed(withdrawSuccessRate, 1)}%",
            defaulterUntil = when {
                bank.isBankrupt() -> "已破产（${DateUtil.formatDateTime(java.util.Date(bank.bankruptAt))}）"
                bank.defaulterUntil > System.currentTimeMillis() && PrivateBankDebtService.outstanding(mainBankDebt) > 0.0001 ->
                    "至少到 ${DateUtil.formatDateTime(java.util.Date(bank.defaulterUntil))} 且债务清零"
                bank.defaulterUntil > System.currentTimeMillis() -> DateUtil.formatDateTime(java.util.Date(bank.defaulterUntil))
                PrivateBankDebtService.outstanding(mainBankDebt) > 0.0001 -> "主银行债务清零后"
                else -> "无"
            },
            fundLines = listOf(
                BankInfoFundLine("准备资金池", MoneyFormatUtil.format(reserve), "主银行账户"),
                BankInfoFundLine("流动资金池", MoneyFormatUtil.format(liquidity), "可周转资金"),
                BankInfoFundLine("放贷库存", MoneyFormatUtil.format(inventory), "冻结额度"),
                BankInfoFundLine("风险保证金", MoneyFormatUtil.format(ownerBankBalance), "行长主银行"),
                BankInfoFundLine("国卷持仓", MoneyFormatUtil.format(govBondPrincipal), "未赎回"),
                BankInfoFundLine("狐卷持仓", MoneyFormatUtil.format(foxBondPrincipal), "未到期")
            ),
            loanLines = buildList {
                add(BankInfoLoanLine("可借额度", MoneyFormatUtil.format(remainingLoanLimit), "${activeOffers.size} 个可借项目"))
                add(BankInfoLoanLine("发布总额", MoneyFormatUtil.format(totalLoanLimit), "当前启用额度"))
                add(BankInfoLoanLine("代收本息", MoneyFormatUtil.format(outstandingLoan), "${activeLoans.size} 笔未结清"))
                add(BankInfoLoanLine("放贷利率", loanOfferInterestRangeText(activeOffers), "最小 / 最大"))
                add(
                    BankInfoLoanLine(
                        "主银行债务",
                        MoneyFormatUtil.format(PrivateBankDebtService.outstanding(mainBankDebt)),
                        "本金 ${MoneyFormatUtil.format(mainBankDebt?.principal ?: 0.0)} / 利息 ${MoneyFormatUtil.format(mainBankDebt?.accruedInterest ?: 0.0)}"
                    )
                )
            }
        )
    }

    private fun loanOfferInterestRangeText(offers: List<cn.chahuyun.economy.model.privatebank.PrivateBankLoanOfferDto>): String {
        if (offers.isEmpty()) return "0%/0%"
        val min = offers.minOf { it.interest }
        val max = offers.maxOf { it.interest }
        return "${formatInterestPercent(min)}/${formatInterestPercent(max)}"
    }

    private fun formatInterestPercent(interest: Int): String {
        val value = FormatUtil.fixed(interest / 10.0, 1).removeSuffix(".0")
        return "$value%"
    }

    private fun formatPrivateBankInfoCard(card: PrivateBankInfoCard): String = buildString {
        append(card.name).append("(code=").append(card.code).append(")\n")
        append("行长：").append(card.owner).append('\n')
        append("星级：").append("★".repeat(card.star.coerceIn(1, 5)))
        append("（").append(card.star.coerceIn(1, 5)).append("）")
        append(" / 评分：").append(card.avgReview).append('\n')
        append("存款利率：").append(card.interest)
        append(" / 存款总额：").append(card.totalDeposit).append('\n')
        append("取款成功率：").append(card.withdrawSuccessRate)
        append(" / 失信至：").append(card.defaulterUntil).append('\n')
        append("描述：").append(card.slogan.ifBlank { "暂无描述" }).append('\n')
        append("\n资金位置：\n")
        card.fundLines.forEach { line ->
            append("- ").append(line.label).append("：").append(line.amount)
            if (line.description.isNotBlank()) append("（").append(line.description).append("）")
            append('\n')
        }
        append("\n额度与放贷：\n")
        if (card.loanLines.isEmpty()) {
            append("- 暂无放贷：0（未发布贷款额度）\n")
        } else {
            card.loanLines.forEach { line ->
                append("- ").append(line.label).append("：").append(line.value)
                if (line.description.isNotBlank()) append("（").append(line.description).append("）")
                append('\n')
            }
        }
    }.trimEnd()

    suspend fun listBanks(event: MessageEvent) {
        val subject: Contact = event.subject
        val bot: Bot = event.bot
        PrivateBankBankruptcyService.processEligibleBanks()
        val banks = PrivateBankRepository.listBanks().filterNot { it.isBankrupt() }
        if (banks.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "当前没有银行"))
            return
        }

        val items = buildBankListItems(subject, banks)
        val builder = ForwardMessageBuilder(subject)
        builder.add(
            bot,
            PlainText("银行列表（按星级、评分、存款排序）\n共 ${items.size} 家银行，展示前 ${items.take(15).size} 家。")
        )
        items.take(15).forEachIndexed { index, item ->
            builder.add(bot, PlainText(formatBankListItem(index + 1, item)))
        }
        if (items.size > 15) {
            builder.add(bot, PlainText("还有 ${items.size - 15} 家银行未展示，可使用 银行信息 <code> 查看详情。"))
        }
        subject.sendMessage(builder.build())
    }

    suspend fun pbCreate(event: MessageEvent) {
        val subject: Contact = event.subject
        val sender = event.sender
        val parts = event.message.contentToString().trim().split(" ", limit = 3)
        if (parts.size < 3) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "用法：银行创建 <code> <name>"))
            return
        }
        val code = parts[1].trim()
        val name = parts[2].trim()
        val (_, msg) = PrivateBankService.createBank(sender, code, name)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    suspend fun pbDesc(event: MessageEvent) {
        val subject = event.subject
        val sender = event.sender

        val ownedBank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == sender.id }
        if (ownedBank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你还没有创建自己的银行"))
            return
        }
        val bank = PrivateBankService.getBank(ownedBank.code) ?: return
        if (bank.isBankrupt()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "银行已经破产，不能修改描述"))
            return
        }

        val group = subject as? Group
        if (group == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "请在群聊中使用该指令"))
            return
        }

        subject.sendMessage(
            MessageUtil.formatMessageChain(
                event.message,
                "请在 180 秒内发送新的银行描述（下一条消息将作为描述）"
            )
        )
        val next = MessageUtil.INSTANCE.nextUserForGroupMessageEvent(group.id, sender.id, 180)
        if (next == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "已超时，未收到新的描述"))
            return
        }

        val content = next.message.contentToString().trim().take(500)
        bank.slogan = content
        PrivateBankRepository.saveBank(bank)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, "描述已更新"))
    }

    suspend fun pbRate(event: MessageEvent) {
        val subject = event.subject
        val sender = event.sender
        val arg = event.message.contentToString().trim().split(" ").last()
        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == sender.id }
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你还没有创建自己的银行"))
            return
        }
        val (_, msg) = PrivateBankService.setDepositorInterest(sender, bank.code, arg)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    suspend fun pbFund(event: MessageEvent) {
        val subject = event.subject
        val parts = event.message.contentToString().trim().split(Regex("\\s+"))
        val amount = parts.getOrNull(1)?.let(MoneyFormatUtil::parse)
        val poolToken = parts.getOrNull(2)
        val transfer = PrivateBankService.parseFundingPoolTransfer(poolToken)
        val pool = if (transfer == null) PrivateBankService.FundingPool.parse(poolToken) else null
        if (amount == null || amount <= 0 || (transfer == null && pool == null)) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "用法：银行补资 <金额> [P|F|PF|FP]，PF=准备金池转流动金池"))
            return
        }
        val (_, msg) = if (transfer != null) {
            PrivateBankService.transferFundingPool(event.sender, amount, transfer)
        } else {
            PrivateBankService.fund(event.sender, amount, requireNotNull(pool))
        }
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    suspend fun pbDivest(event: MessageEvent) {
        val subject = event.subject
        val parts = event.message.contentToString().trim().split(Regex("\\s+"))
        val amount = parts.getOrNull(1)?.let(MoneyFormatUtil::parse)
        val pool = PrivateBankService.FundingPool.parse(parts.getOrNull(2))
        if (amount == null || amount <= 0 || pool == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "用法：银行撤资 <金额> [P|F]，P=准备金池，F=流动金池"))
            return
        }
        val (_, msg) = PrivateBankService.divest(event.sender, amount, pool)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    suspend fun pbProfile(event: MessageEvent) {
        val subject = event.subject
        val parts = event.message.contentToString().trim().split(Regex("\\s+"), limit = 3)
        val code = parts.getOrNull(1)
        val name = parts.getOrNull(2)
        if (code.isNullOrBlank() || name.isNullOrBlank()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "用法：银行资料修改 <code> <name>"))
            return
        }
        val (_, msg) = PrivateBankService.updateBankProfile(event.sender, code, name)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    suspend fun pbLoanOffer(event: MessageEvent) {
        val subject = event.subject
        val sender = event.sender
        val parts = event.message.contentToString().trim().split(Regex("\\s+"))
        val money = parts.getOrNull(1)?.let(MoneyFormatUtil::parse)
        if (money == null || money <= 0) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "用法：放贷 <金额> [利率]，例如：放贷 100000 / 放贷 10M / 放贷 20K"))
            return
        }

        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == sender.id }
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你还没有创建自己的银行"))
            return
        }

        val dedupKey = listOf(sender.id, bank.code, money, event.message.contentToString().trim()).joinToString("|")
        if (!markLoanOfferCommandIfFresh(dedupKey)) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "放贷请求已收到，请勿重复提交"))
            return
        }

        val rateRaw = parts.getOrNull(2)?.toDoubleOrNull() ?: (bank.depositorInterest / 10.0)
        val ratePercent = if (rateRaw > 10) rateRaw / 10.0 else rateRaw
        val (_, msg) = PrivateBankService.publishLoanByPlan(sender, bank.code, money, ratePercent)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    suspend fun pbLoanRate(event: MessageEvent) {
        val subject = event.subject
        val parts = event.message.contentToString().trim().split(Regex("\\s+"))
        val offerId = parts.getOrNull(1)?.toIntOrNull()
        val rate = parts.getOrNull(2)?.toDoubleOrNull()
        if (offerId == null || rate == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "用法：贷款利息修改 <offerId> <1.0-18.0>"))
            return
        }

        val (_, msg) = PrivateBankService.updateLoanOfferInterest(event.sender, offerId, rate)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    suspend fun pbLoanOffers(event: MessageEvent) {
        val subject = event.subject
        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == event.sender.id }
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你还没有创建自己的银行"))
            return
        }

        val allLoans = PrivateBankRepository.listLoansByBank(bank.code)
        val offers = PrivateBankRepository.listLoanOffers(bank.code)
            .filter { offer -> isVisibleLoanOffer(offer, allLoans) }
            .sortedWith(compareByDescending<cn.chahuyun.economy.model.privatebank.PrivateBankLoanOfferDto> { it.enabled }
                .thenByDescending { it.remaining }
                .thenByDescending { it.id })

        if (offers.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "当前没有贷款额度"))
            return
        }

        val msg = buildString {
            append("${bank.name} 的贷款额度\n")
            offers.take(20).forEach { offer ->
                append("#").append(offer.id)
                    .append(" | 启用")
                    .append(" | 剩余=").append(MoneyFormatUtil.format(offer.remaining))
                    .append(" / 总额=").append(MoneyFormatUtil.format(offer.total))
                    .append(" | 日利率=").append(FormatUtil.fixed(offer.interest / 10.0, 1)).append("%")
                    .append(" | 期限=").append(offer.termDays).append("天")
                    .append('\n')
                allLoans.asSequence()
                    .filter { it.offerId == offer.id && isOutstandingLoan(it) }
                    .sortedBy { it.createdAt }
                    .forEach { loan ->
                        append("  ").append(formatLoanBorrower(loan, displayUser(subject, loan.borrowerQq)))
                            .append('\n')
                    }
            }
            if (offers.size > 20) append("还有 ${offers.size - 20} 条未展示\n")
            append("修改：贷款利息修改 <offerId> <1.0-18.0>\n")
            append("撤回：撤贷 <offerId>")
        }.trimEnd()

        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    suspend fun pbBorrowedLoans(event: MessageEvent) {
        val subject = event.subject
        val loans = PrivateBankRepository.listLoansByBorrower(event.sender.id)
            .filter(::isOutstandingLoan)
            .sortedWith(compareBy<PrivateBankLoanDto> { it.dueAt }.thenByDescending { it.id })

        if (loans.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你当前没有借贷记录"))
            return
        }

        val bankNames = PrivateBankRepository.listBanks().associate { it.code to it.name }
        val displayedLoans = loans.take(20)
        val msg = buildString {
            append("我的借贷列表\n")
            displayedLoans.forEach { loan ->
                append(formatBorrowedLoan(loan, bankNames[loan.bankCode]))
                append('\n')
            }
            if (loans.size > displayedLoans.size) {
                append("还有 ${loans.size - displayedLoans.size} 条未展示\n")
            }
        }.trimEnd()

        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    internal fun formatBorrowedLoan(loan: PrivateBankLoanDto, bankName: String?): String {
        val interestAmount = (loan.dueTotal - loan.principal).coerceAtLeast(0.0)
        val outstanding = (loan.dueTotal - loan.repaidAmount).coerceAtLeast(0.0)
        val status = if (loan.repaidAt != 0L || outstanding <= 0.0001) "已结清" else "待还款"
        val bank = bankName?.let { "$it(${loan.bankCode})" } ?: loan.bankCode

        return buildString {
            append("#").append(loan.id)
                .append(" | 银行=").append(bank)
                .append(" | ").append(status)
                .append("\n  本金=").append(MoneyFormatUtil.format(loan.principal))
                .append(" | 日利率=").append(FormatUtil.fixed(loan.interest / 10.0, 1)).append("%")
                .append(" | 利息=").append(MoneyFormatUtil.format(interestAmount))
                .append("\n  已还=").append(MoneyFormatUtil.format(loan.repaidAmount))
                .append(" | 待还=").append(MoneyFormatUtil.format(outstanding))
                .append(" | 最迟还款=").append(DateUtil.formatDateTime(java.util.Date(loan.dueAt)))
        }
    }

    suspend fun pbLoanCancel(event: MessageEvent) {
        val subject = event.subject
        val offerId = event.message.contentToString().trim().split(Regex("\\s+")).getOrNull(1)?.toIntOrNull()
        if (offerId == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "用法：撤贷 <offerId>"))
            return
        }

        val (_, msg) = PrivateBankService.cancelLoanOffer(event.sender, offerId)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    suspend fun defaultBank(event: MessageEvent) {
        val userInfo = UserCoreManager.getUserInfo(event.sender)
        val key = userInfo.defaultPrivateBankCode.trim()
        if (key.isBlank()) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "当前默认银行：主银行"))
            return
        }

        val bank = PrivateBankService.getBank(key)
        val msg = if (bank == null) {
            "当前默认银行：$key（银行不存在或已删除）"
        } else if (bank.isBankrupt()) {
            "当前默认银行：主银行（原银行 ${bank.name} 已破产）"
        } else {
            "当前默认银行：${bank.name}(code=${bank.code})"
        }
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    suspend fun setDefaultBank(event: MessageEvent) {
        val subject = event.subject
        val userInfo = UserCoreManager.getUserInfo(event.sender)
        val parts = event.message.contentToString().trim().split(Regex("\\s+"), limit = 2)
        val key = parts.getOrNull(1)?.trim().orEmpty()
        if (key.isBlank()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "用法：默认银行设置 <code/name|main|主银行>"))
            return
        }

        if (key.equals("main", ignoreCase = true) || key == "主银行") {
            userInfo.defaultPrivateBankCode = ""
            UserCoreManager.saveUserInfo(userInfo)
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "默认银行已切换为：主银行"))
            return
        }

        val bank = PrivateBankService.getBank(key)
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "未找到该银行：$key"))
            return
        }
        if (bank.isBankrupt()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "该银行已经破产，不能设为默认银行"))
            return
        }

        userInfo.defaultPrivateBankCode = bank.code
        UserCoreManager.saveUserInfo(userInfo)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, "默认银行已切换为：${bank.name}(code=${bank.code})"))
    }

    suspend fun foxView(event: MessageEvent) {
        val subject = event.subject
        val bonds = PrivateBankFoxBondService.listActiveBonds()
        if (bonds.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "当前没有可竞标的狐卷"))
            return
        }

        val msg = buildString {
            append("当前可竞标狐卷（最多展示 10 条）\n")
            bonds.take(10).forEach { b ->
                append(
                    "${b.code} | 面额=${MoneyFormatUtil.format(b.faceValue)} | 原始=${
                        FormatUtil.fixed(
                            b.baseRate,
                            2
                        )
                    }%/day | 期限=${b.termDays}天 | 截止=${DateUtil.formatDateTime(java.util.Date(b.bidEndAt))}\n"
                )
            }
            append("用法：狐卷竞标 <code> <溢价金额> <接受利息(%/day)>\n")
            append("示例：狐卷竞标 ")
            append(bonds.first().code)
            append(" 5000000 3.2")
        }
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg.trimEnd()))
    }

    suspend fun foxBid(event: MessageEvent) {
        val subject = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        if (parts.size < 4) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    "用法：狐卷竞标 <code> <溢价金额> <接受利息(%/day)>"
                )
            )
            return
        }
        val code = parts[1]
        val premium = MoneyFormatUtil.parse(parts[2]) ?: 0.0
        val rate = parts[3].toDoubleOrNull() ?: 0.0
        val (_, msg) = PrivateBankFoxBondService.submitBid(event.sender, code, premium, rate)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    suspend fun foxSupplement(event: MessageEvent) {
        val bonds = PrivateBankFoxBondService.supplementBonds()
        event.subject.sendMessage(
            MessageUtil.formatMessageChain(event.message, "狐卷补发完成，本次新增 ${bonds.size} 张狐卷")
        )
    }

    suspend fun pbInfo(event: MessageEvent) {
        val raw = event.message.contentToString().trim()
        val parts = raw.split(" ")
        val userInfo= UserCoreManager.getUserInfo(event.sender)

        // 带参：展示指定银行详情
        val arg = parts.getOrNull(1)?.trim().takeIf { !it.isNullOrBlank() }
        if (arg != null) {
            sendBankInfo(event, arg)
            return
        }

        // 不带参：优先展示自己拥有的私银；否则展示默认私银；都没有则跳过
        val ownerBank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == event.sender.id }
        val key = ownerBank?.code ?: userInfo.defaultPrivateBankCode.trim().takeIf { it.isNotBlank() }
        if (key.isNullOrBlank()) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你还没有创建自己的银行，也没有默认银行"))
            return
        }
        sendBankInfo(event, key)
    }

    suspend fun myBank(event: MessageEvent) {
        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == event.sender.id }
        if (bank == null) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你没有创建银行"))
            return
        }
        sendBankInfo(event, bank.code)
    }

    suspend fun myBankText(event: MessageEvent) {
        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == event.sender.id }
        if (bank == null) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你没有创建银行"))
            return
        }
        sendBankInfoText(event, bank)
    }

    suspend fun pbReview(event: MessageEvent) {
        val subject = event.subject
        val raw = event.message.contentToString().trim()
        val parts = raw.split(" ")
        val rating = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val rest = parts.drop(2)

        val userInfo = UserCoreManager.getUserInfo(event.sender)
        var bankKey: String? = null
        var content: String? = null

        if (rest.isNotEmpty()) {
            val last = rest.last()
            val bank = PrivateBankService.getBank(last)
            if (bank != null) {
                bankKey = bank.code
                content = rest.dropLast(1).joinToString(" ").trim().takeIf { it.isNotBlank() }
            } else {
                content = rest.joinToString(" ").trim().takeIf { it.isNotBlank() }
            }
        }
        if (bankKey.isNullOrBlank()) bankKey = userInfo.defaultPrivateBankCode
        if (bankKey.isNullOrBlank()) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    "请指定银行：银行评分 <1-5> <描述> [code/name]"
                )
            )
            return
        }

        val (_, msg) = PrivateBankService.addReview(event.sender, bankKey, rating, content)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    suspend fun pbBorrow(event: MessageEvent) {
        val subject = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        val amount = parts.getOrNull(1)?.let(MoneyFormatUtil::parse) ?: 0.0
        val userInfo = UserCoreManager.getUserInfo(event.sender)
        val key = parts.getOrNull(2) ?: userInfo.defaultPrivateBankCode
        if (key.isNullOrBlank()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "请指定银行：贷款 <金额> [code/name]"))
            return
        }

        val (_, msg) = PrivateBankService.borrowFromBank(event.sender, key, amount)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    suspend fun pbRepay(event: MessageEvent) {
        val subject = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        val amount = parts.getOrNull(1)?.let(MoneyFormatUtil::parse) ?: 0.0
        val userInfo = UserCoreManager.getUserInfo(event.sender)
        val key = parts.getOrNull(2) ?: userInfo.defaultPrivateBankCode
        if (key.isNullOrBlank()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "请指定银行：还款 <金额> [code/name]"))
            return
        }

        val (_, msg) = PrivateBankService.repayToBankByAmount(event.sender, key, amount)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

}
