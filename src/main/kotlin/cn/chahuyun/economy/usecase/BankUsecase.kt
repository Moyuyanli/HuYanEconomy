package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.entity.bank.BankInfo
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.privatebank.PrivateBankService
import cn.chahuyun.economy.utils.*
import cn.chahuyun.hibernateplus.HibernateFactory
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText
import xyz.cssxsh.mirai.economy.service.EconomyAccount
import kotlin.math.floor

/**
 * 银行相关用例（主银行 + 默认私银路由）。
 */
object BankUsecase {

    suspend fun deposit(event: MessageEvent) {
        val userInfo: UserInfo = UserCoreManager.getUserInfo(event.sender)
        val user = userInfo.user
        val subject: Contact = event.subject

        val message = event.message
        val singleMessages: MessageChainBuilder = MessageUtil.quoteReply(message)
        val code = message.serializeToMiraiCode()

        val money = code.split(" ")[1].toInt()

        // 若用户设置了默认私银，则无参“存款”优先存入私银
        val defaultPb = userInfo.defaultPrivateBankCode?.trim().takeIf { !it.isNullOrBlank() }
        if (!defaultPb.isNullOrBlank()) {
            val (_, msg) = PrivateBankService.deposit(user, defaultPb, money.toDouble())
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
            return
        }
        val moneyByUser = EconomyUtil.getMoneyByUser(user)
        if (moneyByUser - money < 0) {
            singleMessages.append("你的金币不够${MoneyFormatUtil.format(money.toDouble())}了")
            subject.sendMessage(singleMessages.build())
            return
        }

        if (EconomyUtil.turnUserToBank(user, money.toDouble())) {
            singleMessages.append("存款成功!")
            subject.sendMessage(singleMessages.build())
        } else {
            singleMessages.append("存款失败!")
            subject.sendMessage(singleMessages.build())
            Log.error("银行管理:存款失败!")
        }
    }

    suspend fun mainBankDeposit(event: MessageEvent) {
        val userInfo: UserInfo = UserCoreManager.getUserInfo(event.sender)
        val user = userInfo.user
        val subject: Contact = event.subject
        val message = event.message
        val singleMessages: MessageChainBuilder = MessageUtil.quoteReply(message)
        val code = message.serializeToMiraiCode()

        val money = code.split(" ")[1].toInt()
        val moneyByUser = EconomyUtil.getMoneyByUser(user)
        if (moneyByUser - money < 0) {
            singleMessages.append("你的金币不够${MoneyFormatUtil.format(money.toDouble())}了")
            subject.sendMessage(singleMessages.build())
            return
        }

        if (EconomyUtil.turnUserToBank(user, money.toDouble())) {
            singleMessages.append("存款成功!")
            subject.sendMessage(singleMessages.build())
        } else {
            singleMessages.append("存款失败!")
            subject.sendMessage(singleMessages.build())
            Log.error("银行管理:存款失败!")
        }
    }

    suspend fun privateBankDeposit(event: MessageEvent) {
        val userInfo: UserInfo = UserCoreManager.getUserInfo(event.sender)
        val user = userInfo.user
        val subject: Contact = event.subject

        val parts = event.message.contentToString().trim().split(" ")
        val amount = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        val bankKey = parts.getOrNull(2) ?: ""
        val (ok, msg) = PrivateBankService.deposit(user, bankKey, amount)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
        if (ok) {
            val bank = PrivateBankService.getBank(bankKey)
            if (bank != null) {
                userInfo.defaultPrivateBankCode = bank.code
                HibernateFactory.merge(userInfo)
            }
        }
    }

    suspend fun depositAllInteger(event: MessageEvent) {
        val user = event.sender
        val subject: Contact = event.subject

        val message = event.message
        val singleMessages: MessageChainBuilder = MessageUtil.quoteReply(message)

        val wallet = EconomyUtil.getMoneyByUser(user)
        val amount = floor(wallet).toInt()
        if (amount <= 0) {
            singleMessages.append("你的钱包没有可存入的整数金币")
            subject.sendMessage(singleMessages.build())
            return
        }

        if (EconomyUtil.turnUserToBank(user, amount.toDouble())) {
            singleMessages.append("已一键存入 ${amount} 金币")
            subject.sendMessage(singleMessages.build())
        } else {
            singleMessages.append("一键存款失败!")
            subject.sendMessage(singleMessages.build())
            Log.error("银行管理:一键存款失败")
        }
    }

    suspend fun withdrawal(event: MessageEvent) {
        val userInfo: UserInfo = UserCoreManager.getUserInfo(event.sender)
        val user = userInfo.user
        val subject: Contact = event.subject

        val message = event.message
        val singleMessages: MessageChainBuilder = MessageUtil.quoteReply(message)
        val code = message.serializeToMiraiCode()

        val money = code.split(" ")[1].toInt()

        // 若用户设置了默认私银，则无参“取款”优先从私银取出
        val defaultPb = userInfo.defaultPrivateBankCode?.trim().takeIf { !it.isNullOrBlank() }
        if (!defaultPb.isNullOrBlank()) {
            val (_, msg) = PrivateBankService.withdraw(user, defaultPb, money.toDouble())
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
            return
        }
        val moneyByBank = EconomyUtil.getMoneyByBank(user)
        if (moneyByBank - money < 0) {
            singleMessages.append("你的银行余额不够${MoneyFormatUtil.format(money.toDouble())}枚金币了")
            subject.sendMessage(singleMessages.build())
            return
        }

        if (EconomyUtil.turnBankToUser(user, money.toDouble())) {
            singleMessages.append("取款成功!")
            subject.sendMessage(singleMessages.build())
        } else {
            singleMessages.append("取款失败!")
            subject.sendMessage(singleMessages.build())
            Log.error("银行管理:取款失败!")
        }
    }

    suspend fun mainBankWithdraw(event: MessageEvent) {
        val user = event.sender
        val subject: Contact = event.subject
        val message = event.message
        val singleMessages: MessageChainBuilder = MessageUtil.quoteReply(message)
        val code = message.serializeToMiraiCode()

        val money = code.split(" ")[1].toInt()
        val moneyByBank = EconomyUtil.getMoneyByBank(user)
        if (moneyByBank - money < 0) {
            singleMessages.append("你的银行余额不够${MoneyFormatUtil.format(money.toDouble())}枚金币了")
            subject.sendMessage(singleMessages.build())
            return
        }

        if (EconomyUtil.turnBankToUser(user, money.toDouble())) {
            singleMessages.append("取款成功!")
            subject.sendMessage(singleMessages.build())
        } else {
            singleMessages.append("取款失败!")
            subject.sendMessage(singleMessages.build())
            Log.error("银行管理:取款失败!")
        }
    }

    suspend fun privateBankWithdraw(event: MessageEvent) {
        val userInfo: UserInfo = UserCoreManager.getUserInfo(event.sender)
        val user = userInfo.user
        val subject: Contact = event.subject

        val parts = event.message.contentToString().trim().split(" ")
        val amount = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        val bankKey = parts.getOrNull(2) ?: ""
        val (ok, msg) = PrivateBankService.withdraw(user, bankKey, amount)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
        if (ok) {
            val bank = PrivateBankService.getBank(bankKey)
            if (bank != null) {
                userInfo.defaultPrivateBankCode = bank.code
                HibernateFactory.merge(userInfo)
            }
        }
    }

    suspend fun viewBankInterest(event: MessageEvent) {
        Log.info("银行指令")

        val bankInfo = HibernateFactory.selectOneById(BankInfo::class.java, 1)
        if (bankInfo == null) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "银行信息未初始化"))
            return
        }
        val weeklyRate = bankInfo.interest / 10.0
        event.subject.sendMessage(
            MessageUtil.formatMessageChain(
                event.message,
                "本周银行利率是${FormatUtil.fixed(weeklyRate, 1)}%"
            )
        )
    }

    suspend fun viewRegalTop(event: MessageEvent) {
        Log.info("经济指令")

        val subject = event.subject
        val bot: Bot = event.bot

        val builder = ForwardMessageBuilder(subject)
        builder.add(bot, PlainText("以下是银行存款排行榜:"))

        val accountByBank: Map<EconomyAccount, Double> = EconomyUtil.getAccountByBank()
        val totalBankMoney = EconomyUtil.getBankTotalCached()

        // 全局银行可能存在同一用户的多个子账户（不同 description）。富豪榜需要按用户聚合，避免同一人多次上榜。
        val userTotals: List<Pair<String, Double>> = accountByBank.entries
            .groupBy({ it.key.uuid }, { it.value })
            .map { (uuid, values) -> uuid to values.sum() }
            .sortedByDescending { it.second }
            .take(10)

        var index = 1
        for ((uuid, money) in userTotals) {
            val userInfo = HibernateFactory.selectOneById(UserInfo::class.java, uuid)
            if (userInfo == null) continue
            val group: Group? = bot.getGroup(userInfo.registerGroup)
            val groupName = group?.name ?: "未找到群"
            val groupDisplay = "${userInfo.registerGroup} (${groupName})"
            val ratio = if (totalBankMoney > 0) money / totalBankMoney * 100 else 0.0

            val plainText = MessageUtil.formatMessage(
                "top:${index++}\n" +
                    "用户:${userInfo.name ?: "未知"}\n" +
                    "注册群:${groupDisplay}\n" +
                    "存款:${MoneyFormatUtil.format(money)}\n" +
                    "占比:${FormatUtil.fixed(ratio, 1)}%"
            )
            builder.add(bot, plainText)
        }

        subject.sendMessage(builder.build())
    }
}
