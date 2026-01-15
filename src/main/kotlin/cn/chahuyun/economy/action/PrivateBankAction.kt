package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.privatebank.PrivateBankRepository
import cn.chahuyun.economy.privatebank.PrivateBankService
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.ShareUtils
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 私人银行指令入口
 *
 * 说明：本模块尽量不影响现有主银行(存款/取款)逻辑。
 */
@EventComponent
class PrivateBankAction {

    @MessageAuthorize(text = ["私人银行 列表", "私银 列表"])
    suspend fun listBanks(event: MessageEvent) {
        val subject: Contact = event.subject
        val banks = PrivateBankRepository.listBanks().sortedByDescending { it.star }
        if (banks.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "当前没有私人银行"))
            return
        }

        val msg = buildString {
            append("私人银行列表（按星级排序）\n")
            banks.take(15).forEachIndexed { idx, b ->
                append(
                    String.format(
                        "%d) %s | code=%s | ⭐%d | 利率=%.1f%% | 失信=%s\n",
                        idx + 1,
                        b.name,
                        b.code,
                        b.star,
                        b.depositorInterest / 10.0,
                        if (b.isDefaulter()) "是" else "否"
                    )
                )
            }
        }
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg.trimEnd()))
    }

    @MessageAuthorize(text = ["私人银行 信息 .+", "私银 信息 .+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun bankInfo(event: MessageEvent) {
        val subject: Contact = event.subject
        val code = event.message.contentToString().trim().split(" ").last()
        val bank = PrivateBankRepository.findBankByCode(code)
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "未找到该私人银行：$code"))
            return
        }

        val deposits = PrivateBankRepository.listDeposits(bank.code)
        val totalDeposit = deposits.sumOf { it.principal }

        val msg = MessageUtil.formatMessage(
            "私人银行信息\n" +
                "名称:%s\n" +
                "code:%s\n" +
                "行长:%d\n" +
                "星级:%d\n" +
                "利率:%.1f%%\n" +
                "平均评分:%.2f\n" +
                "存款总额:%.1f\n" +
                "取款成功率:%.1f%%\n" +
                "失信至:%s",
            bank.name,
            bank.code,
            bank.ownerQq,
            bank.star,
            bank.depositorInterest / 10.0,
            bank.avgReview,
            ShareUtils.rounding(totalDeposit),
            if (bank.withdrawRequests <= 0) 100.0 else (100.0 * (bank.withdrawRequests - bank.withdrawFailures) / bank.withdrawRequests.toDouble()),
            bank.defaulterUntil?.let { DateUtil.formatDateTime(it) } ?: "无"
        )
        subject.sendMessage(MessageUtil.quoteReply(event.message).append(msg).build())
    }

    @MessageAuthorize(text = ["私人银行 创建 .+", "私银 创建 .+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun createBank(event: MessageEvent) {
        val subject: Contact = event.subject
        val sender = event.sender

        val name = event.message.contentToString().trim().split(" ", limit = 3).last().trim()
        val (ok, msg) = PrivateBankService.createBank(sender, name)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
        if (ok) {
            // 确保 userInfo 初始化
            UserCoreManager.getUserInfo(sender)
        }
    }

    @MessageAuthorize(text = ["私存 \\S+ \\d+(\\.\\d+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun deposit(event: MessageEvent) {
        val subject: Contact = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        if (parts.size < 3) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "用法：私存 <bankCode> <金额>"))
            return
        }
        val bankCode = parts[1]
        val amount = parts[2].toDoubleOrNull() ?: 0.0
        val (ok, msg) = PrivateBankService.deposit(event.sender, bankCode, amount)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
        if (ok) {
            UserCoreManager.getUserInfo(event.sender)
        }
    }

    @MessageAuthorize(text = ["私取 \\S+ \\d+(\\.\\d+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun withdraw(event: MessageEvent) {
        val subject: Contact = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        if (parts.size < 3) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "用法：私取 <bankCode> <金额>"))
            return
        }
        val bankCode = parts[1]
        val amount = parts[2].toDoubleOrNull() ?: 0.0
        val (ok, msg) = PrivateBankService.withdraw(event.sender, bankCode, amount)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
        if (ok) {
            UserCoreManager.getUserInfo(event.sender)
        }
    }

    @MessageAuthorize(text = ["私银 评价 \\S+ [1-5]"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun review(event: MessageEvent) {
        val subject: Contact = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        val bankCode = parts.getOrNull(2) ?: return
        val rating = parts.last().toIntOrNull() ?: 0
        val (_, msg) = PrivateBankService.addReview(event.sender, bankCode, rating)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    @MessageAuthorize(text = ["私银 设利率 \\S+ -?\\d+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun setInterest(event: MessageEvent) {
        val subject: Contact = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        val bankCode = parts.getOrNull(2) ?: return
        val interest = parts.getOrNull(3)?.toIntOrNull()
        if (interest == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "用法：私银 设利率 <bankCode> <整数利率>（显示利率=interest/10 %%）"))
            return
        }

        val bank = PrivateBankRepository.findBankByCode(bankCode)
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "未找到该私人银行"))
            return
        }
        if (bank.ownerQq != event.sender.id) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "只有行长可以设置利率"))
            return
        }
        if (bank.isDefaulter()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "失信期间禁止调整利率"))
            return
        }

        bank.depositorInterest = interest
        PrivateBankRepository.saveBank(bank)
        PrivateBankService.refreshRating(bank.code)
        subject.sendMessage(
            MessageUtil.formatMessageChain(
                event.message,
                "设置成功：当前利率 %.1f%%",
                bank.depositorInterest / 10.0
            )
        )
    }

    // ===== 国卷 =====

    @MessageAuthorize(text = ["国卷 查看", "国卷"])
    suspend fun viewBondIssue(event: MessageEvent) {
        val subject: Contact = event.subject
        val issue = PrivateBankService.ensureWeeklyBondIssue()
            subject.sendMessage(
            MessageUtil.formatMessageChain(
                event.message,
                "本周国卷：week=%s 剩余=%.1f 锁仓=%d天 倍数=%.2fx",
                issue.weekKey,
                ShareUtils.rounding(issue.remaining),
                issue.lockDays,
                issue.rateMultiplier
            )
        )
    }

    @MessageAuthorize(text = ["国卷 购买 \\S+ \\d+(\\.\\d+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun buyBond(event: MessageEvent) {
        val subject: Contact = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        val bankCode = parts.getOrNull(2) ?: return
        val amount = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0
        val (_, msg) = PrivateBankService.buyBond(event.sender, bankCode, amount)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    @MessageAuthorize(text = ["国卷 赎回 \\d+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun redeemBond(event: MessageEvent) {
        val subject: Contact = event.subject
        val holdingId = event.message.contentToString().trim().split(" ").last().toIntOrNull() ?: 0
        val (_, msg) = PrivateBankService.redeemBond(event.sender, holdingId)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    // ===== 贷款 =====

    @MessageAuthorize(text = ["贷款 发布 \\S+ \\d+(\\.\\d+)? \\d+ \\d+ (LIQUIDITY|OWNER)"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun publishLoan(event: MessageEvent) {
        val subject: Contact = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        val bankCode = parts.getOrNull(2) ?: return
        val total = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0
        val interest = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val days = parts.getOrNull(5)?.toIntOrNull() ?: 0
        val source = parts.getOrNull(6) ?: "LIQUIDITY"
        val (_, msg) = PrivateBankService.publishLoan(event.sender, bankCode, total, interest, days, source)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    @MessageAuthorize(text = ["贷款 借款 \\d+ \\d+(\\.\\d+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun borrow(event: MessageEvent) {
        val subject: Contact = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        val offerId = parts.getOrNull(2)?.toIntOrNull() ?: 0
        val amount = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0
        val (_, msg) = PrivateBankService.borrow(event.sender, offerId, amount)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    @MessageAuthorize(text = ["贷款 还款 \\d+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun repay(event: MessageEvent) {
        val subject: Contact = event.subject
        val loanId = event.message.contentToString().trim().split(" ").last().toIntOrNull() ?: 0
        val (_, msg) = PrivateBankService.repay(event.sender, loanId)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }
}
