package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.privatebank.PrivateBankFoxBondService
import cn.chahuyun.economy.privatebank.PrivateBankRepository
import cn.chahuyun.economy.privatebank.PrivateBankService
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.ShareUtils
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.date.DateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 私人银行指令入口
 *
 * 说明：本模块尽量不影响现有主银行(存款/取款)逻辑。
 */
@EventComponent
class PrivateBankAction {

    private suspend fun sendBankInfo(event: MessageEvent, bankKey: String) {
        val subject: Contact = event.subject
        val bank = PrivateBankService.getBank(bankKey)
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "未找到该私人银行：$bankKey"))
            return
        }

        val deposits = PrivateBankRepository.listDeposits(bank.code)
        val totalDeposit = deposits.sumOf { it.principal }

        val msg = MessageUtil.formatMessage(
            "私人银行信息\n" +
                "名称:%s\n" +
                "code:%s\n" +
                "描述:%s\n" +
                "行长:%d\n" +
                "星级:%d\n" +
                "利率:%.1f%%\n" +
                "平均评分:%.2f\n" +
                "存款总额:%.1f\n" +
                "取款成功率:%.1f%%\n" +
                "失信至:%s",
            bank.name,
            bank.code,
            bank.slogan ?: "无",
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
        val codeOrName = event.message.contentToString().trim().split(" ").last()
        sendBankInfo(event, codeOrName)
    }

    @MessageAuthorize(text = ["私人银行 创建 .+", "私银 创建 .+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun createBank(event: MessageEvent) {
        val subject: Contact = event.subject
        val sender = event.sender

        val name = event.message.contentToString().trim().split(" ", limit = 3).last().trim()
        val (ok, msg) = PrivateBankService.createBank(sender, null, name)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
        if (ok) {
            // 确保 userInfo 初始化
            val userInfo = UserCoreManager.getUserInfo(sender)
            // 若创建成功且未设置默认私银，则自动设为自己创建的私银
            val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == sender.id }
            if (bank != null && userInfo.defaultPrivateBankCode.isNullOrBlank()) {
                userInfo.defaultPrivateBankCode = bank.code
                HibernateFactory.merge(userInfo)
            }
        }
    }

    // ===== 2.0.0 计划指令兼容层 =====

    @MessageAuthorize(text = ["银行创建 \\S+ .+", "银行 创建 \\S+ .+"], messageMatching = MessageMatchingEnum.REGULAR)
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

    @MessageAuthorize(text = ["银行描述修改", "银行 描述修改"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbDesc(event: MessageEvent) {
        val subject = event.subject
        val sender = event.sender

        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == sender.id }
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你还没有创建私人银行"))
            return
        }

        val group = subject as? Group
        if (group == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "请在群聊中使用该指令"))
            return
        }

        subject.sendMessage(MessageUtil.formatMessageChain(event.message, "请在 180 秒内发送新的银行描述（下一条消息将作为描述）"))
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

    @MessageAuthorize(text = ["银行利率变更 \\S+", "银行 利率变更 \\S+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbRate(event: MessageEvent) {
        val subject = event.subject
        val sender = event.sender
        val arg = event.message.contentToString().trim().split(" ").last()
        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == sender.id }
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你还没有创建私人银行"))
            return
        }
        val (_, msg) = PrivateBankService.setDepositorInterest(sender, bank.code, arg)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    @MessageAuthorize(text = ["银行放贷 \\d+(\\.\\d+)? \\d+(\\.\\d+)?", "银行 放贷 \\d+(\\.\\d+)? \\d+(\\.\\d+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbLoanOffer(event: MessageEvent) {
        val subject = event.subject
        val sender = event.sender
        val parts = event.message.contentToString().trim().split(" ")
        val money = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        val rateRaw = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0

        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == sender.id }
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你还没有创建私人银行"))
            return
        }

        val ratePercent = if (rateRaw > 10) rateRaw / 10.0 else rateRaw
        val (_, msg) = PrivateBankService.publishLoanByPlan(sender, bank.code, money, ratePercent)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    @MessageAuthorize(text = ["狐卷 查看", "狐卷"], messageMatching = MessageMatchingEnum.REGULAR)
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
                    "${b.code} | 面额=${ShareUtils.rounding(b.faceValue)} | 原始=${String.format("%.2f", b.baseRate)}%/day | 期限=${b.termDays}天 | 截止=${DateUtil.formatDateTime(b.bidEndAt)}\n"
                )
            }
            append("用法：狐卷竞标 <code> <溢价金额> <接受利息(%/day)>\n")
            append("示例：狐卷竞标 ")
            append(bonds.first().code)
            append(" 5000000 3.2")
        }
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg.trimEnd()))
    }

    @MessageAuthorize(
        text = [
            "狐卷竞标 \\S+ \\d+(\\.\\d+)? \\d+(\\.\\d+)?"
        ],
        messageMatching = MessageMatchingEnum.REGULAR
    )
    suspend fun foxBid(event: MessageEvent) {
        val subject = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        if (parts.size < 4) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "用法：狐卷竞标 <code> <溢价金额> <接受利息(%/day)>"))
            return
        }
        val code = parts[1]
        val premium = parts[2].toDoubleOrNull() ?: 0.0
        val rate = parts[3].toDoubleOrNull() ?: 0.0
        val (_, msg) = PrivateBankFoxBondService.submitBid(event.sender, code, premium, rate)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    @MessageAuthorize(text = ["银行信息", "银行 信息"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbInfo(event: MessageEvent) {
        val raw = event.message.contentToString().trim()
        val parts = raw.split(" ")
        val userInfo: UserInfo = UserCoreManager.getUserInfo(event.sender)
        val key = parts.getOrNull(1)?.trim().takeIf { !it.isNullOrBlank() }
            ?: userInfo.defaultPrivateBankCode
            ?: return
        sendBankInfo(event, key)
    }

    @MessageAuthorize(text = ["银行列表", "银行 列表"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbList(event: MessageEvent) {
        val raw = event.message.contentToString().trim()
        val parts = raw.split(" ")
        if (parts.size >= 2) {
            // 填参时：按计划语义直接当作查询
            sendBankInfo(event, parts[1])
            return
        }
        val ownerBank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == event.sender.id } ?: return
        sendBankInfo(event, ownerBank.code)
    }

    @MessageAuthorize(text = ["银行评分 [1-5]( .+)?", "银行 评分 [1-5]( .+)?"], messageMatching = MessageMatchingEnum.REGULAR)
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
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "请指定银行：银行评分 <1-5> <描述> [code/name]"))
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

    @MessageAuthorize(text = ["贷款 \\d+(\\.\\d+)?( \\S+)?", "借款 \\d+(\\.\\d+)?( \\S+)?"], messageMatching = MessageMatchingEnum.REGULAR)
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

    @MessageAuthorize(text = ["还款 \\d+(\\.\\d+)?( \\S+)?"], messageMatching = MessageMatchingEnum.REGULAR)
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
        val (_, msg) = PrivateBankService.addReview(event.sender, bankCode, rating, null)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    @MessageAuthorize(text = ["私银 设利率 \\S+ -?\\d+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun setInterest(event: MessageEvent) {
        val subject: Contact = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        val bankCode = parts.getOrNull(2) ?: return
        val arg = parts.getOrNull(3)
        if (arg.isNullOrBlank()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "用法：私银 设利率 <bankCode> <rate/max/min/now>"))
            return
        }

        val (_, msg) = PrivateBankService.setDepositorInterest(event.sender, bankCode, arg)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
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
