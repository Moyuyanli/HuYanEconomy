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
     * 购买国卷：行长用流动金池资金购买本周国卷
     */
    suspend fun buyBond(event: MessageEvent) {
        val subject: Contact = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        val amount = parts.getOrNull(1)?.let(MoneyFormatUtil::parse) ?: 0.0
        if (amount <= 0) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "用法：国卷购买 <金额>"))
            return
        }

        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == event.sender.id }
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你还没有创建自己的银行"))
            return
        }

        val (_, msg) = PrivateBankService.buyBond(event.sender, bank.code, amount)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    /**
     * 赎回国卷；带 ID 时赎回指定持仓，不带 ID 时尝试赎回全部持仓。
     */
    suspend fun redeemBond(event: MessageEvent) {
        val subject: Contact = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        val holdingId = parts.getOrNull(1)?.toIntOrNull()

        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == event.sender.id }
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你还没有创建自己的银行"))
            return
        }

        if (holdingId != null) {
            // 赎回指定持仓
            val (_, msg) = PrivateBankService.redeemBond(event.sender, holdingId)
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
        } else {
            // 赎回全部到期持仓
            val holdings = PrivateBankRepository.listBondHoldings(bank.code)
                .filter { it.redeemedAt == 0L }

            if (holdings.isEmpty()) {
                subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你没有国债持仓"))
                return
            }

            var successCount = 0
            val results = mutableListOf<String>()

            for (h in holdings) {
                val (ok, msg) = PrivateBankService.redeemBond(event.sender, h.id)
                if (ok) {
                    successCount++
                    results.add("持仓#${h.id}: $msg")
                } else {
                    results.add("持仓#${h.id}: $msg")
                }
            }

            val summary = buildString {
                append("国卷赎回结果（共 ${holdings.size} 笔）\n")
                results.forEach { append("$it\n") }
            }
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, summary.trimEnd()))
        }
    }

    /**
     * 查看本周国卷发行信息 + 本行持仓列表
     */
    suspend fun bondList(event: MessageEvent) {
        val subject: Contact = event.subject
        val issue = PrivateBankService.ensureWeeklyBondIssue()

        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == event.sender.id }

        val msg = buildString {
            append("本周国卷信息\n")
            append("期号: ${issue.weekKey}\n")
            append("利率倍数: ${FormatUtil.fixed(issue.rateMultiplier, 2)}x\n")
            append("锁仓天数: ${issue.lockDays} 天\n")
            append("总额度: ${MoneyFormatUtil.format(issue.totalLimit)}\n")
            append("剩余额度: ${MoneyFormatUtil.format(issue.remaining)}\n")

            if (bank != null) {
                val holdings = PrivateBankRepository.listBondHoldings(bank.code)
                    .filter { it.redeemedAt == 0L }
                if (holdings.isNotEmpty()) {
                    append("\n你的银行持仓（${bank.name}）\n")
                    holdings.forEach { h ->
                        val dueAt = java.util.Date(h.boughtAt + h.lockDays * 86400000L)
                        val isExpired = dueAt.before(java.util.Date())
                        val status = if (isExpired) "已到期" else "未到期"
                        append("  #${h.id} | 金额=${MoneyFormatUtil.format(h.principal)} | ${h.rateMultiplier}x | $status\n")
                    }
                } else {
                    append("\n你的银行暂无国卷持仓\n")
                }
                append("\n用法：国卷购买 <金额> | 国卷赎回 [持仓ID]")
            } else {
                append("\n你还没有创建银行，无法购买国债")
            }
        }
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg.trimEnd()))
    }
}
