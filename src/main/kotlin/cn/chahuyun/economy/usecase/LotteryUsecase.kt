package cn.chahuyun.economy.usecase

import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.entity.LotteryInfo
import cn.chahuyun.economy.manager.LotteryManager
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.MoneyFormatUtil
import cn.chahuyun.hibernateplus.HibernateFactory
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain

/**
 * 彩票相关用例。
 */
object LotteryUsecase {

    suspend fun addLottery(event: GroupMessageEvent) {
        Log.info("彩票指令")

        val user: User = event.sender
        val subject = event.subject

        val message: MessageChain = event.message
        val code = message.serializeToMiraiCode()

        val split = code.split(" ")
        var number = StringBuilder(split[1])

        val money = split[2].toDouble()

        val moneyByUser = EconomyUtil.getMoneyByUser(user)
        if (moneyByUser - money <= 0) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "你都穷的叮当响了，还来猜签？"))
            return
        }

        val type: Int
        val typeString: String
        when (number.length) {
            3 -> {
                type = 1
                typeString = "小签"
            }

            4 -> {
                type = 2
                typeString = "中签"
            }

            5 -> {
                type = 3
                typeString = "大签"
            }

            else -> {
                subject.sendMessage(MessageUtil.formatMessageChain(message, "猜签类型错误!"))
                return
            }
        }

        if (type == 1) {
            if (!(0 < money && money <= 1000)) {
                subject.sendMessage(MessageUtil.formatMessageChain(message, "你投注的金额不属于这个签!"))
                return
            }
        } else if (type == 2) {
            if (!(0 < money && money <= 10000)) {
                subject.sendMessage(MessageUtil.formatMessageChain(message, "你投注的金额不属于这个签!"))
                return
            }
        } else {
            if (!(0 < money && money <= 1000000)) {
                subject.sendMessage(MessageUtil.formatMessageChain(message, "你投注的金额不属于这个签!"))
                return
            }
        }

        val string = number.toString()
        val chars = string.toCharArray()
        number = StringBuilder(chars[0].toString())
        for (i in 1 until string.length) {
            val aByte = chars[i].toString()
            number.append(",").append(aByte)
        }

        val lotteryInfo = LotteryInfo(user.id, subject.id, money, type, number.toString())
        if (!EconomyUtil.minusMoneyToUser(user, money)) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "猜签失败！"))
            return
        }
        HibernateFactory.merge(lotteryInfo)
        subject.sendMessage(
            MessageUtil.formatMessageChain(
                message,
                "猜签成功:\n" +
                    "猜签类型:${typeString}\n" +
                    "猜签号码:${number}\n" +
                    "猜签金币:${MoneyFormatUtil.format(money)}"
            )
        )

        if (type == 1) {
            LotteryManager.ensureMinutesSchedule()
        } else if (type == 2) {
            LotteryManager.ensureHoursSchedule()
        }
    }

    suspend fun startLottery(event: GroupMessageEvent) {
        val group: Group = event.group
        val util = PermUtil
        val user = UserUtil.group(group.id)

        if (util.checkUserHasPerm(user, EconPerm.LOTTERY_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的猜签已经开启了!"))
            return
        }

        if (util.addUserToPermGroupByName(user, EconPerm.GROUP.LOTTERY_PERM_GROUP)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的猜签开启成功!"))
        } else {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的猜签开启失败!"))
        }
    }

    suspend fun endLottery(event: GroupMessageEvent) {
        val group: Group = event.group
        val util = PermUtil
        val user = UserUtil.group(group.id)

        if (!util.checkUserHasPerm(user, EconPerm.LOTTERY_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的猜签已经关闭!"))
            return
        }

        val permGroup: PermGroup = util.takePermGroupByName(EconPerm.GROUP.LOTTERY_PERM_GROUP)
        permGroup.users.remove(user)
        permGroup.save()

        group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的猜签关闭成功!"))
    }
}
