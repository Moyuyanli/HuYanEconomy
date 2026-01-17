@file:Suppress("DuplicatedCode")

package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.privatebank.PrivateBankFoxBondService
import cn.chahuyun.economy.privatebank.PrivateBankRepository
import cn.chahuyun.economy.privatebank.PrivateBankService
import cn.chahuyun.economy.utils.FormatUtil
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.MoneyFormatUtil
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.date.DateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 银行用例（代码层模块名仍为 PrivateBank；用户侧统一称为“银行”）。
 */
object PrivateBankUsecase {

    private fun displayUser(subject: Contact, qq: Long): String {
        val nick = (subject as? Group)
            ?.get(qq)
            ?.nameCardOrNick
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        return if (nick != null) "$nick($qq)" else qq.toString()
    }

    private suspend fun sendBankInfo(event: MessageEvent, bankKey: String) {
        val subject: Contact = event.subject
        val bank = PrivateBankService.getBank(bankKey)
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "未找到该银行：$bankKey"))
            return
        }

        val deposits = PrivateBankRepository.listDeposits(bank.code)
        val totalDeposit = deposits.sumOf { it.principal }

        val withdrawSuccessRate = if (bank.withdrawRequests <= 0) {
            100.0
        } else {
            100.0 * (bank.withdrawRequests - bank.withdrawFailures) / bank.withdrawRequests.toDouble()
        }

        val msg = MessageUtil.formatMessage(
            "银行信息\n" +
                    "名称:${bank.name}\n" +
                    "code:${bank.code}\n" +
                    "描述:${bank.slogan ?: "无"}\n" +
                    "行长:${displayUser(subject, bank.ownerQq)}\n" +
                    "星级:${bank.star}\n" +
                    "利率:${FormatUtil.fixed(bank.depositorInterest / 10.0, 1)}%\n" +
                    "平均评分:${FormatUtil.fixed(bank.avgReview, 2)}\n" +
                    "存款总额:${MoneyFormatUtil.format(totalDeposit)}\n" +
                    "取款成功率:${FormatUtil.fixed(withdrawSuccessRate, 1)}%\n" +
                    "失信至:${bank.defaulterUntil?.let { DateUtil.formatDateTime(it) } ?: "无"}"
        )
        subject.sendMessage(MessageUtil.quoteReply(event.message).append(msg).build())
    }

    suspend fun listBanks(event: MessageEvent) {
        val subject: Contact = event.subject
        val banks = PrivateBankRepository.listBanks().sortedByDescending { it.star }
        if (banks.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "当前没有银行"))
            return
        }

        val msg = buildString {
            append("银行列表（按星级排序）\n")
            banks.take(15).forEachIndexed { idx, b ->
                append(
                    "${idx + 1}) ${b.name} | code=${b.code} | ⭐${b.star} | " +
                            "利率=${FormatUtil.fixed(b.depositorInterest / 10.0, 1)}% | " +
                            "失信=${if (b.isDefaulter()) "是" else "否"}\n"
                )
            }
        }
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg.trimEnd()))
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
        val (ok, msg) = PrivateBankService.createBank(sender, code, name)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
        if (ok) {
            val userInfo = UserCoreManager.getUserInfo(sender)
            userInfo.defaultPrivateBankCode = code
            HibernateFactory.merge(userInfo)
        }
    }

    suspend fun pbDesc(event: MessageEvent) {
        val subject = event.subject
        val sender = event.sender

        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == sender.id }
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你还没有创建自己的银行"))
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
        val next = withContext(Dispatchers.IO) {
            MessageUtil.INSTANCE.nextUserForGroupMessageEventSync(group.id, sender.id, 180)
        }
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

    suspend fun pbLoanOffer(event: MessageEvent) {
        val subject = event.subject
        val sender = event.sender
        val parts = event.message.contentToString().trim().split(" ")
        val money = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        val rateRaw = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0

        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == sender.id }
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你还没有创建自己的银行"))
            return
        }

        val ratePercent = if (rateRaw > 10) rateRaw / 10.0 else rateRaw
        val (_, msg) = PrivateBankService.publishLoanByPlan(sender, bank.code, money, ratePercent)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
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
                    }%/day | 期限=${b.termDays}天 | 截止=${DateUtil.formatDateTime(b.bidEndAt)}\n"
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
        val premium = parts[2].toDoubleOrNull() ?: 0.0
        val rate = parts[3].toDoubleOrNull() ?: 0.0
        val (_, msg) = PrivateBankFoxBondService.submitBid(event.sender, code, premium, rate)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    suspend fun pbInfo(event: MessageEvent) {
        val raw = event.message.contentToString().trim()
        val parts = raw.split(" ")
        val userInfo: UserInfo = UserCoreManager.getUserInfo(event.sender)

        // 带参：展示指定银行详情
        val arg = parts.getOrNull(1)?.trim().takeIf { !it.isNullOrBlank() }
        if (arg != null) {
            sendBankInfo(event, arg)
            return
        }

        // 不带参：优先展示自己拥有的私银；否则展示默认私银；都没有则跳过
        val ownerBank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == event.sender.id }
        val key = ownerBank?.code ?: userInfo.defaultPrivateBankCode ?: return
        sendBankInfo(event, key)
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

        val bank = PrivateBankService.getBank(bankKey)
        if (bank != null) {
            userInfo.defaultPrivateBankCode = bank.code
            HibernateFactory.merge(userInfo)
        }
    }

    suspend fun pbBorrow(event: MessageEvent) {
        val subject = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        val amount = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        val userInfo = UserCoreManager.getUserInfo(event.sender)
        val key = parts.getOrNull(2) ?: userInfo.defaultPrivateBankCode
        if (key.isNullOrBlank()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "请指定银行：贷款 <金额> [code/name]"))
            return
        }

        val (ok, msg) = PrivateBankService.borrowFromBank(event.sender, key, amount)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
        if (ok) {
            val bank = PrivateBankService.getBank(key)
            if (bank != null) {
                userInfo.defaultPrivateBankCode = bank.code
                HibernateFactory.merge(userInfo)
            }
        }
    }

    suspend fun pbRepay(event: MessageEvent) {
        val subject = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        val amount = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
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
