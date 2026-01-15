package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.entity.bank.BankInfo
import cn.chahuyun.economy.manager.BankInterestTask
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.cron.CronUtil
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText
import xyz.cssxsh.mirai.economy.service.EconomyAccount

/**
 * 银行管理
 * 存款 | 取款 | 富豪榜
 */
@EventComponent
class BankAction {

    companion object {
        /**
         * 初始化银行
         * 应当开启银行利息定时器
         */
        @JvmStatic
        fun init() {
            val one = HibernateFactory.selectOneById(BankInfo::class.java, 1)
            if (one == null) {
                val bankInfo = BankInfo(
                    "global",
                    "主银行",
                    "经济服务",
                    HuYanEconomy.config.owner,
                    0.0
                )
                HibernateFactory.merge(bankInfo)
            }

            val bankInfos = try {
                HibernateFactory.selectList(BankInfo::class.java)
            } catch (e: Exception) {
                Log.error("银行管理:利息加载出错!", e)
                emptyList()
            }

            val bankInterestTask = BankInterestTask("bank", bankInfos ?: emptyList())
            CronUtil.schedule("bank", "0 0 4 * * ?", bankInterestTask)
        }
    }

    /**
     * 存款
     */
    @MessageAuthorize(text = ["存款 \\d+", "deposit \\d+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun deposit(event: MessageEvent) {
        val userInfo: UserInfo = UserCoreManager.getUserInfo(event.sender)
        val user = userInfo.user
        val subject: Contact = event.subject

        val message = event.message
        val singleMessages: MessageChainBuilder = MessageUtil.quoteReply(message)
        val code = message.serializeToMiraiCode()

        val money = code.split(" ")[1].toInt()
        val moneyByUser = EconomyUtil.getMoneyByUser(user)
        if (moneyByUser - money < 0) {
            singleMessages.append(String.format("你的金币不够%s了", money))
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

    /**
     * 取款
     */
    @MessageAuthorize(text = ["取款 \\d+", "withdraw \\d+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun withdrawal(event: MessageEvent) {
        val userInfo: UserInfo = UserCoreManager.getUserInfo(event.sender)
        val user = userInfo.user
        val subject: Contact = event.subject

        val message = event.message
        val singleMessages: MessageChainBuilder = MessageUtil.quoteReply(message)
        val code = message.serializeToMiraiCode()

        val money = code.split(" ")[1].toInt()
        val moneyByBank = EconomyUtil.getMoneyByBank(user)
        if (moneyByBank - money < 0) {
            singleMessages.append(String.format("你的银行余额不够%s枚金币了", money))
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

    /**
     * 查看利率
     */
    @MessageAuthorize(text = ["本周利率", "银行利率"])
    suspend fun viewBankInterest(event: MessageEvent) {
        Log.info("银行指令")

        val bankInfo = HibernateFactory.selectOneById(BankInfo::class.java, 1)
        if (bankInfo == null) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "银行信息未初始化"))
            return
        }
        event.subject.sendMessage(
            MessageUtil.formatMessageChain(
                event.message,
                "本周银行利率是%.1f%%",
                bankInfo.interest / 10.0
            )
        )
    }

    /**
     * 富豪榜
     */
    @MessageAuthorize(text = ["富豪榜", "经济排行"])
    suspend fun viewRegalTop(event: MessageEvent) {
        Log.info("经济指令")

        val subject = event.subject
        val bot: Bot = event.bot

        val builder = ForwardMessageBuilder(subject)
        builder.add(bot, PlainText("以下是银行存款排行榜:"))

        val accountByBank: Map<EconomyAccount, Double> = EconomyUtil.getAccountByBank()
        val totalBankMoney = EconomyUtil.getBankTotalCached()

        val collect = accountByBank.entries
            .sortedByDescending { it.value }
            .take(10)

        var index = 1
        for (entry in collect) {
            val userInfo: UserInfo = UserCoreManager.getUserInfo(entry.key)
            val group: Group? = bot.getGroup(userInfo.registerGroup)
            val groupName = group?.name ?: "未找到群"
            val groupDisplay = String.format("%s (%s)", userInfo.registerGroup, groupName)
            val ratio = if (totalBankMoney > 0) entry.value / totalBankMoney * 100 else 0.0

            val plainText = MessageUtil.formatMessage(
                "top:%d%n用户:%s%n注册群:%s%n存款:%.1f%n占比:%.1f%%",
                index++,
                userInfo.name ?: "未知",
                groupDisplay,
                entry.value,
                ratio
            )
            builder.add(bot, plainText)
        }

        subject.sendMessage(builder.build())
    }
}
