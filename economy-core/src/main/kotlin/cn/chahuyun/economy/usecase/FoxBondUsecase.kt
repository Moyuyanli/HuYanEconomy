package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.data.repository.PrivateBankRepository
import cn.chahuyun.economy.privatebank.PrivateBankFoxBondService
import cn.chahuyun.economy.privatebank.PrivateBankService
import cn.chahuyun.economy.utils.FormatUtil
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.MoneyFormatUtil
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import java.util.*

/**
 * 国卷/狐卷债券相关用例。
 */
object FoxBondUsecase {

    /**
     * 查看当前可竞标的狐卷列表
     */
    suspend fun foxView(event: MessageEvent) {
        val subject: Contact = event.subject
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
                        FormatUtil.fixed(b.baseRate, 2)
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

    /**
     * 提交狐卷竞标
     */
    suspend fun foxBid(event: MessageEvent) {
        val subject: Contact = event.subject
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

    /**
     * 购买国卷：行长用流动金池资金购买指定 code 的国卷
     */
    suspend fun buyBond(event: MessageEvent) {
        val subject: Contact = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        val amount = parts.getOrNull(1)?.let(MoneyFormatUtil::parse) ?: 0.0
        val code = parts.getOrNull(2).orEmpty()
        if (amount <= 0 || code.isBlank()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "用法：购买国卷 <金额> <code>"))
            return
        }

        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == event.sender.id }
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你还没有创建自己的银行"))
            return
        }

        val (_, msg) = PrivateBankService.buyBond(event.sender, bank.code, code, amount)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    /**
     * 赎回国卷；不带金额时赎回该 code 的全部持仓，带金额时赎回指定本金。
     */
    suspend fun redeemBond(event: MessageEvent) {
        val subject: Contact = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        val code = parts.getOrNull(1).orEmpty()
        val amount = parts.getOrNull(2)?.let(MoneyFormatUtil::parse)
        if (code.isBlank()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "用法：赎回国卷 <code> [金额]"))
            return
        }

        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == event.sender.id }
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你还没有创建自己的银行"))
            return
        }

        val (_, msg) = PrivateBankService.redeemBond(event.sender, code, amount)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    /** 查看仍可购买的国卷，每个转发页最多 15 个。 */
    suspend fun bondList(event: MessageEvent) {
        val subject: Contact = event.subject
        PrivateBankService.ensureDailyBondIssues()
        val issues = PrivateBankService.listAvailableBondIssues()
        if (issues.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "当前没有可购买的国卷"))
            return
        }

        val lines = issues.map { issue ->
            val code = issue.code.ifBlank { issue.weekKey }
            buildString {
                append(code)
                append("\n剩余：${MoneyFormatUtil.format(issue.remaining)} / ${MoneyFormatUtil.format(issue.totalLimit)}")
                append("\n利率：${FormatUtil.fixed(issue.rateMultiplier, 2)}%/day")
                append("\n锁仓：${issue.lockDays} 天")
                append("\n赎回时间：${DateUtil.formatDateTime(Date(PrivateBankService.bondRedeemAt(issue)))}")
            }
        }
        sendForwardPages(event, "可购买国卷", lines)
    }

    /** 查看当前发送者作为行长所拥有银行的国卷持仓。 */
    suspend fun bondHoldings(event: MessageEvent) {
        val subject = event.subject
        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == event.sender.id }
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你还没有创建自己的银行"))
            return
        }

        val issueById = PrivateBankRepository.listBondIssues().associateBy { it.id }
        val holdings = PrivateBankRepository.listBondHoldings(bank.code)
            .filter { it.redeemedAt == 0L && it.principal > 0.0001 }
            .sortedBy { it.boughtAt + it.lockDays * DAY_MILLIS }
        if (holdings.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "${bank.name} 暂无国卷持仓"))
            return
        }

        val now = System.currentTimeMillis()
        val lines = holdings.map { holding ->
            val issue = issueById[holding.issueId]
            val code = issue?.code?.ifBlank { issue.weekKey } ?: "#${holding.issueId}"
            val dueAt = holding.boughtAt + holding.lockDays * DAY_MILLIS
            buildString {
                append(code)
                append("\n本金：${MoneyFormatUtil.format(holding.principal)}")
                append("\n利率：${FormatUtil.fixed(holding.rateMultiplier, 2)}%/day")
                append("\n购买时间：${DateUtil.formatDateTime(Date(holding.boughtAt))}")
                append("\n赎回时间：${DateUtil.formatDateTime(Date(dueAt))}")
                append("\n状态：${if (dueAt <= now) "可赎回" else "锁仓中"}")
            }
        }
        sendForwardPages(event, "${bank.name} 国卷持仓", lines)
    }

    suspend fun supplementBonds(event: MessageEvent) {
        val issues = PrivateBankService.supplementDailyBondIssues()
        event.subject.sendMessage(
            MessageUtil.formatMessageChain(event.message, "国卷补发完成，本次新增 ${issues.size} 个国卷")
        )
    }

    private suspend fun sendForwardPages(event: MessageEvent, title: String, lines: List<String>) {
        val pages = lines.chunked(PAGE_SIZE)
        pages.forEachIndexed { pageIndex, page ->
            val builder = ForwardMessageBuilder(event.subject)
            builder.add(event.bot, PlainText("$title ${pageIndex + 1}/${pages.size}（共 ${lines.size} 个）"))
            page.forEach { builder.add(event.bot, PlainText(it)) }
            event.subject.sendMessage(builder.build())
        }
    }

    private const val PAGE_SIZE = 15
    private const val DAY_MILLIS = 86_400_000L
}
