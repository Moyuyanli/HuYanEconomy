@file:Suppress("DuplicatedCode")

package cn.chahuyun.economy.feature.privatebank

import cn.chahuyun.economy.actionchain.annotation.Action
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.data.repository.PrivateBankRepository
import cn.chahuyun.economy.feature.user.UserManager
import cn.chahuyun.economy.image.PrivateBankInfoImageRenderer
import cn.chahuyun.economy.image.model.BankInfoFundLine
import cn.chahuyun.economy.image.model.BankInfoLoanLine
import cn.chahuyun.economy.image.model.PrivateBankInfoCard
import cn.chahuyun.economy.model.privatebank.PrivateBankDto
import cn.chahuyun.economy.model.privatebank.PrivateBankLoanDto
import cn.chahuyun.economy.model.privatebank.PrivateBankLoanOfferDto
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
 * 私银功能的主用例：开户、资金池、放贷、借贷、狐卷、评分、默认银行。
 *
 * 代码层模块名仍为 PrivateBank，用户侧统一称为「银行」。
 *
 * ## 动作清单（23 个）
 * | 分组 | actionId |
 * |---|---|
 * | 查询 | `core.privatebank.list` / `.info` / `.my` / `.myText` |
 * | 开户与资料 | `.create` / `.desc` / `.profile` / `.rate` / `.review` |
 * | 资金池 | `.fund` / `.divest` |
 * | 放贷 | `.loan.offer` / `.loan.rate` / `.loan.list` / `.loan.cancel` |
 * | 借贷 | `.loan.borrowed` / `.borrow` / `.repay` |
 * | 狐卷 | `.fox.view` / `.fox.bid` / `.fox.supplement` |
 * | 默认银行 | `.default.show` / `.default.set` |
 *
 * 参数解析、行长身份、破产与去重判定全部在 [PrivateBankRules] 里前置完成，
 * 本类只负责调用 `PrivateBankService` 并把返回的消息发出去。
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

    /**
     * 格式化一条「我的借贷」记录。
     *
     * @param loan 借贷记录。
     * @param bankName 银行名称；查不到时只显示 code。
     */
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

    /**
     * 发送银行信息卡片（图片）；渲染失败时降级成一句提示，不影响其他指令。
     *
     * @param actor 本次指令的参与者快照（发送目标与引用消息）。
     * @param bankKey 银行 code 或名称。
     */
    private suspend fun sendBankInfo(actor: PrivateBankActor, bankKey: String) {
        val subject: Contact = actor.subject
        val bank = PrivateBankService.getBank(bankKey)
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "未找到该银行：$bankKey"))
            return
        }
        val card = buildPrivateBankInfoCard(subject, bank)

        try {
            val image = PrivateBankInfoImageRenderer.render(card)
            ImageMessageUtil.sendQuotedImage(subject, actor.message, image)
        } catch (e: Exception) {
            Log.error("银行信息图片生成或发送失败", e)
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    actor.message,
                    "银行信息生成失败：${bank.name}(code=${bank.code})"
                )
            )
        }
    }

    /**
     * 汇总一家私银的资金、额度与借贷视图，供图片与纯文字两版共用。
     *
     * @param subject 消息上下文（解析行长昵称用）。
     * @param bank 银行实体。
     */
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

    private fun loanOfferInterestRangeText(offers: List<PrivateBankLoanOfferDto>): String {
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

    /**
     * 查看银行列表（`core.privatebank.list`）。
     *
     * @param event 消息事件（取 bot 构造转发）。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.privatebank.list")
    suspend fun listBanks(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:列表 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val subject: Contact = actor.subject
        PrivateBankBankruptcyService.processEligibleBanks()
        val banks = PrivateBankRepository.listBanks().filterNot { it.isBankrupt() }
        if (banks.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "当前没有银行"))
            return
        }

        val items = buildBankListItems(subject, banks)
        val builder = ForwardMessageBuilder(subject)
        builder.add(
            event.bot,
            PlainText("银行列表（按星级、评分、存款排序）\n共 ${items.size} 家银行，展示前 ${items.take(15).size} 家。")
        )
        items.take(15).forEachIndexed { index, item ->
            builder.add(event.bot, PlainText(formatBankListItem(index + 1, item)))
        }
        if (items.size > 15) {
            builder.add(event.bot, PlainText("还有 ${items.size - 15} 家银行未展示，可使用 银行信息 <code> 查看详情。"))
        }
        subject.sendMessage(builder.build())
    }

    /**
     * 创建银行（`core.privatebank.create`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankCreatePlan] 与参与者快照。
     */
    @Action("core.privatebank.create")
    suspend fun pbCreate(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:创建 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val plan = context.get(PrivateBankCreatePlan::class)
        if (plan == null) {
            Log.warning("银行:创建规则未产出参数，本次终止（用户 ${event.sender.id}）")
            return
        }

        val (_, msg) = PrivateBankService.createBank(actor.sender, plan.code, plan.name)
        actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }

    /**
     * 修改银行描述（`core.privatebank.desc`）。
     *
     * 描述通过「180 秒内的下一条群消息」收集，因此这里必须等待用户输入；
     * 行长身份、是否破产、是否群聊都已在规则里判定。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankOwnedBank] 与参与者快照。
     */
    @Action("core.privatebank.desc")
    suspend fun pbDesc(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:改描述 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val owned = context.get(PrivateBankOwnedBank::class)
        if (owned == null) {
            Log.warning("银行:改描述规则未产出行长银行，本次终止（用户 ${event.sender.id}）")
            return
        }

        val subject = actor.subject
        val group = subject as Group

        subject.sendMessage(
            MessageUtil.formatMessageChain(
                actor.message,
                "请在 180 秒内发送新的银行描述（下一条消息将作为描述）"
            )
        )
        val next = MessageUtil.INSTANCE.nextUserForGroupMessageEvent(group.id, actor.sender.id, 180)
        if (next == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "已超时，未收到新的描述"))
            return
        }

        val bank = PrivateBankRepository.findBankByCode(owned.bankCode) ?: return
        bank.slogan = next.message.contentToString().trim().take(500)
        PrivateBankRepository.saveBank(bank)
        subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "描述已更新"))
    }

    /**
     * 修改存款利率（`core.privatebank.rate`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankOwnedBank] 与参与者快照。
     */
    @Action("core.privatebank.rate")
    suspend fun pbRate(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:改利率 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val owned = context.get(PrivateBankOwnedBank::class)
        if (owned == null) {
            Log.warning("银行:改利率规则未产出行长银行，本次终止（用户 ${event.sender.id}）")
            return
        }

        val arg = actor.content.trim().split(" ").last()
        val (_, msg) = PrivateBankService.setDepositorInterest(actor.sender, owned.bankCode, arg)
        actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }

    /**
     * 补充资金池 / 池间划转（`core.privatebank.fund`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankPoolPlan] 与参与者快照。
     */
    @Action("core.privatebank.fund")
    suspend fun pbFund(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:补资 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val plan = context.get(PrivateBankPoolPlan::class)
        if (plan == null) {
            Log.warning("银行:补资规则未产出参数，本次终止（用户 ${event.sender.id}）")
            return
        }

        val (_, msg) = if (plan.transfer != null) {
            PrivateBankService.transferFundingPool(actor.sender, plan.amount, plan.transfer)
        } else {
            PrivateBankService.fund(actor.sender, plan.amount, requireNotNull(plan.pool))
        }
        actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }

    /**
     * 从资金池撤资（`core.privatebank.divest`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankPoolPlan] 与参与者快照。
     */
    @Action("core.privatebank.divest")
    suspend fun pbDivest(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:撤资 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val plan = context.get(PrivateBankPoolPlan::class)
        if (plan == null || plan.pool == null) {
            Log.warning("银行:撤资规则未产出参数，本次终止（用户 ${event.sender.id}）")
            return
        }

        val (_, msg) = PrivateBankService.divest(actor.sender, plan.amount, plan.pool)
        actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }

    /**
     * 修改银行名称（`core.privatebank.profile`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankProfilePlan] 与参与者快照。
     */
    @Action("core.privatebank.profile")
    suspend fun pbProfile(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:改资料 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val plan = context.get(PrivateBankProfilePlan::class)
        if (plan == null) {
            Log.warning("银行:改资料规则未产出参数，本次终止（用户 ${event.sender.id}）")
            return
        }

        val (_, msg) = PrivateBankService.updateBankProfile(actor.sender, plan.code, plan.name)
        actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }

    /**
     * 发布放贷额度（`core.privatebank.loan.offer`）。
     *
     * 去重与利率归一化在规则里完成，重复提交不会走到这里。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankOfferPlan] 与参与者快照。
     */
    @Action("core.privatebank.loan.offer")
    suspend fun pbLoanOffer(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:放贷 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val plan = context.get(PrivateBankOfferPlan::class)
        if (plan == null) {
            Log.warning("银行:放贷规则未产出参数，本次终止（用户 ${event.sender.id}）")
            return
        }

        val (_, msg) = PrivateBankService.publishLoanByPlan(
            actor.sender, plan.bankCode, plan.amount, plan.ratePercent
        )
        actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }

    /**
     * 修改放贷利率（`core.privatebank.loan.rate`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankOfferRatePlan] 与参与者快照。
     */
    @Action("core.privatebank.loan.rate")
    suspend fun pbLoanRate(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:改放贷利率 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val plan = context.get(PrivateBankOfferRatePlan::class)
        if (plan == null) {
            Log.warning("银行:改放贷利率规则未产出参数，本次终止（用户 ${event.sender.id}）")
            return
        }

        val (_, msg) = PrivateBankService.updateLoanOfferInterest(actor.sender, plan.offerId, plan.rate)
        actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }

    /**
     * 查看本行的放贷额度与借款人（`core.privatebank.loan.list`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankOwnedBank] 与参与者快照。
     */
    @Action("core.privatebank.loan.list")
    suspend fun pbLoanOffers(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:放贷列表 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val owned = context.get(PrivateBankOwnedBank::class)
        if (owned == null) {
            Log.warning("银行:放贷列表规则未产出行长银行，本次终止（用户 ${event.sender.id}）")
            return
        }

        val subject = actor.subject
        val allLoans = PrivateBankRepository.listLoansByBank(owned.bankCode)
        val offers = PrivateBankRepository.listLoanOffers(owned.bankCode)
            .filter { offer -> isVisibleLoanOffer(offer, allLoans) }
            .sortedWith(
                compareByDescending<PrivateBankLoanOfferDto> { it.enabled }
                    .thenByDescending { it.remaining }
                    .thenByDescending { it.id }
            )

        if (offers.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "当前没有贷款额度"))
            return
        }

        val msg = buildString {
            append("${owned.bankName} 的贷款额度\n")
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

        subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }

    /**
     * 查看自己的借贷记录（`core.privatebank.loan.borrowed`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.privatebank.loan.borrowed")
    suspend fun pbBorrowedLoans(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:借贷列表 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val subject = actor.subject
        val loans = PrivateBankRepository.listLoansByBorrower(actor.sender.id)
            .filter(::isOutstandingLoan)
            .sortedWith(compareBy<PrivateBankLoanDto> { it.dueAt }.thenByDescending { it.id })

        if (loans.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "你当前没有借贷记录"))
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

        subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }

    /**
     * 撤回放贷额度（`core.privatebank.loan.cancel`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankOfferIdPlan] 与参与者快照。
     */
    @Action("core.privatebank.loan.cancel")
    suspend fun pbLoanCancel(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:撤贷 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val plan = context.get(PrivateBankOfferIdPlan::class)
        if (plan == null) {
            Log.warning("银行:撤贷规则未产出参数，本次终止（用户 ${event.sender.id}）")
            return
        }

        val (_, msg) = PrivateBankService.cancelLoanOffer(actor.sender, plan.offerId)
        actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }

    /**
     * 查看当前默认银行（`core.privatebank.default.show`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.privatebank.default.show")
    suspend fun defaultBank(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:默认银行 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val key = actor.userInfo.defaultPrivateBankCode.trim()
        if (key.isBlank()) {
            actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "当前默认银行：主银行"))
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
        actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }

    /**
     * 设置默认银行（`core.privatebank.default.set`）。
     *
     * 目标银行的存在性、是否破产都已在规则里判定，这里只写回用户资料。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankDefaultPlan] 与参与者快照。
     */
    @Action("core.privatebank.default.set")
    suspend fun setDefaultBank(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:设置默认银行 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val plan = context.get(PrivateBankDefaultPlan::class)
        if (plan == null) {
            Log.warning("银行:设置默认银行规则未产出计划，本次终止（用户 ${event.sender.id}）")
            return
        }

        val userInfo = actor.userInfo
        userInfo.defaultPrivateBankCode = plan.bankKey
        UserManager.saveUserInfo(userInfo)

        val msg = if (plan.bankKey.isBlank()) {
            "默认银行已切换为：主银行"
        } else {
            "默认银行已切换为：${plan.bankName}(code=${plan.bankKey})"
        }
        actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }

    /**
     * 查看可竞标的狐卷（`core.privatebank.fox.view`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.privatebank.fox.view")
    suspend fun foxView(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:狐卷查看 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val subject = actor.subject
        val bonds = PrivateBankFoxBondService.listActiveBonds()
        if (bonds.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "当前没有可竞标的狐卷"))
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
        subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg.trimEnd()))
    }

    /**
     * 提交狐卷竞标（`core.privatebank.fox.bid`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankFoxBidPlan] 与参与者快照。
     */
    @Action("core.privatebank.fox.bid")
    suspend fun foxBid(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:狐卷竞标 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val plan = context.get(PrivateBankFoxBidPlan::class)
        if (plan == null) {
            Log.warning("银行:狐卷竞标规则未产出参数，本次终止（用户 ${event.sender.id}）")
            return
        }

        val (_, msg) = PrivateBankFoxBondService.submitBid(actor.sender, plan.code, plan.premium, plan.rate)
        actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }

    /**
     * 补发狐卷（`core.privatebank.fox.supplement`，入口已限 owner 权限）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.privatebank.fox.supplement")
    suspend fun foxSupplement(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:狐卷补发 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val bonds = PrivateBankFoxBondService.supplementBonds()
        actor.subject.sendMessage(
            MessageUtil.formatMessageChain(actor.message, "狐卷补发完成，本次新增 ${bonds.size} 张狐卷")
        )
    }

    /**
     * 查看银行详情（`core.privatebank.info`）。
     *
     * 目标是银行信息卡片（图片），失败时降级为提示文本。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankInfoTarget] 与参与者快照。
     */
    @Action("core.privatebank.info")
    suspend fun pbInfo(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:银行信息 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val target = context.get(PrivateBankInfoTarget::class)
        if (target == null) {
            Log.warning("银行:银行信息规则未产出目标，本次终止（用户 ${event.sender.id}）")
            return
        }

        sendBankInfo(actor, target.bankKey)
    }

    /**
     * 查看自己的银行（图片，`core.privatebank.my`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankOwnedBank] 与参与者快照。
     */
    @Action("core.privatebank.my")
    suspend fun myBank(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:我的银行 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val owned = context.get(PrivateBankOwnedBank::class) ?: return
        sendBankInfo(actor, owned.bankCode)
    }

    /**
     * 查看自己的银行（纯文字，`core.privatebank.myText`）。
     *
     * 图片渲染失败或客户端不方便看图时使用，字段与图片版一致。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankOwnedBank] 与参与者快照。
     */
    @Action("core.privatebank.myText")
    suspend fun myBankText(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:我的银行(文字) 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val owned = context.get(PrivateBankOwnedBank::class) ?: return
        val bank = PrivateBankRepository.findBankByCode(owned.bankCode) ?: return
        actor.subject.sendMessage(
            MessageUtil.formatMessageChain(
                actor.message,
                formatPrivateBankInfoCard(buildPrivateBankInfoCard(actor.subject, bank))
            )
        )
    }

    /**
     * 给银行打分（`core.privatebank.review`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankReviewPlan] 与参与者快照。
     */
    @Action("core.privatebank.review")
    suspend fun pbReview(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:评分 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val plan = context.get(PrivateBankReviewPlan::class)
        if (plan == null) {
            Log.warning("银行:评分规则未产出参数，本次终止（用户 ${event.sender.id}）")
            return
        }

        val (_, msg) = PrivateBankService.addReview(actor.sender, plan.bankKey, plan.rating, plan.content)
        actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }

    /**
     * 从银行借贷（`core.privatebank.borrow`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankDebtPlan] 与参与者快照。
     */
    @Action("core.privatebank.borrow")
    suspend fun pbBorrow(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:借贷 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val plan = context.get(PrivateBankDebtPlan::class)
        if (plan == null) {
            Log.warning("银行:借贷规则未产出参数，本次终止（用户 ${event.sender.id}）")
            return
        }

        val (_, msg) = PrivateBankService.borrowFromBank(actor.sender, plan.bankKey, plan.amount)
        actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }

    /**
     * 向银行还款（`core.privatebank.repay`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [PrivateBankDebtPlan] 与参与者快照。
     */
    @Action("core.privatebank.repay")
    suspend fun pbRepay(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:还款 用户(${event.sender.id})")

        val actor = context.provide(PrivateBankActor::class, PrivateBankProvider.ACTOR)
        val plan = context.get(PrivateBankDebtPlan::class)
        if (plan == null) {
            Log.warning("银行:还款规则未产出参数，本次终止（用户 ${event.sender.id}）")
            return
        }

        val (_, msg) = PrivateBankService.repayToBankByAmount(actor.sender, plan.bankKey, plan.amount)
        actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }
}
