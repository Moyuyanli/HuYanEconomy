package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.entity.LotteryInfo
import cn.chahuyun.economy.manager.LotteryDayTask
import cn.chahuyun.economy.manager.LotteryHoursTask
import cn.chahuyun.economy.manager.LotteryMinutesTask
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.cron.CronUtil
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 彩票管理
 * 3种彩票：
 * 一分钟开一次
 * 一小时开一次
 * 一天开一次
 */
@EventComponent
class LotteryAction {

    @MessageAuthorize(
        text = ["猜签 (\\d+)( \\d+)", "lottery (\\d+)( \\d+)"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.LOTTERY_PERM]
    )
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
                "猜签成功:\n猜签类型:%s\n猜签号码:%s\n猜签金币:%s",
                typeString,
                number,
                money
            )
        )

        if (type == 1) {
            extractedMinutes()
        } else if (type == 2) {
            extractedHours()
        }
    }

    @MessageAuthorize(text = ["开启 猜签"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun startLottery(event: GroupMessageEvent) {
        val group: Group = event.group
        val util = PermUtil.INSTANCE
        val user = UserUtil.INSTANCE.group(group.id)

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

    @MessageAuthorize(text = ["关闭 猜签"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun endLottery(event: GroupMessageEvent) {
        val group: Group = event.group
        val util = PermUtil.INSTANCE
        val user = UserUtil.INSTANCE.group(group.id)

        if (!util.checkUserHasPerm(user, EconPerm.LOTTERY_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的猜签已经关闭!"))
            return
        }

        val permGroup: PermGroup = util.takePermGroupByName(EconPerm.GROUP.LOTTERY_PERM_GROUP)
        permGroup.users.remove(user)
        permGroup.save()

        group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的猜签关闭成功!"))
    }

    companion object {
        @JvmField
        val minuteTiming = AtomicBoolean(false)

        @JvmField
        val hoursTiming = AtomicBoolean(false)

        /**
         * 初始化彩票
         */
        @JvmStatic
        fun init() {
            val lotteryInfos = try {
                HibernateFactory.selectList(LotteryInfo::class.java)
            } catch (e: Exception) {
                Log.error("彩票管理:彩票初始化失败!", e)
                return
            }

            val minutesLottery = HashMap<String, LotteryInfo>()
            val hoursLottery = HashMap<String, LotteryInfo>()
            val dayLottery = HashMap<String, LotteryInfo>()

            for (lotteryInfo in lotteryInfos) {
                when (lotteryInfo.type) {
                    1 -> {
                        minutesLottery[lotteryInfo.number] = lotteryInfo
                        continue
                    }

                    2 -> {
                        hoursLottery[lotteryInfo.number] = lotteryInfo
                        continue
                    }

                    3 -> dayLottery[lotteryInfo.number] = lotteryInfo
                }
            }

            if (minutesLottery.isNotEmpty()) {
                extractedMinutes()
            }

            if (hoursLottery.isNotEmpty()) {
                extractedHours()
            }

            if (dayLottery.isNotEmpty()) {
                val dayTaskId = "dayTask"
                CronUtil.remove(dayTaskId)
                val dayTask = LotteryDayTask(dayTaskId)
                CronUtil.schedule(dayTaskId, "0 0 0 * * ?", dayTask)
            }
        }

        private fun extractedHours() {
            if (hoursTiming.get()) {
                return
            }
            val hoursTaskId = "hoursTask"
            CronUtil.remove(hoursTaskId)
            val hoursTask = LotteryHoursTask(hoursTaskId)
            CronUtil.schedule(hoursTaskId, "0 0 * * * ?", hoursTask)
            hoursTiming.set(true)
        }

        private fun extractedMinutes() {
            if (minuteTiming.get()) {
                return
            }
            val minutesTaskId = "minutesTask"
            CronUtil.remove(minutesTaskId)
            val minutesTask = LotteryMinutesTask(minutesTaskId)
            CronUtil.schedule(minutesTaskId, "0 * * * * ?", minutesTask)
            minuteTiming.set(true)
        }

        /**
         * 发送彩票结果信息
         */
        @JvmStatic
        fun result(type: Int, location: Int, lotteryInfo: LotteryInfo) {
            if (location == 0) {
                lotteryInfo.remove()
                return
            }
            val bot: Bot = HuYanEconomy.INSTANCE.bot ?: return
            val group = bot.getGroup(lotteryInfo.group) ?: return
            val member: NormalMember = group.get(lotteryInfo.qq) ?: return
            lotteryInfo.remove()

            if (!EconomyUtil.plusMoneyToUser(member, lotteryInfo.bonus)) {
                member.sendMessage("奖金添加失败，请联系管理员!")
                return
            }

            member.sendMessage(lotteryInfo.toMessage())
            when (type) {
                1 -> if (location == 3) {
                    group.sendMessage(
                        String.format(
                            "得签着:%s(%s),奖励%s金币",
                            member.nick,
                            member.id,
                            lotteryInfo.bonus
                        )
                    )
                }

                2 -> if (location == 4) {
                    group.sendMessage(
                        String.format(
                            "得签着:%s(%s),奖励%s金币",
                            member.nick,
                            member.id,
                            lotteryInfo.bonus
                        )
                    )
                }

                3 -> if (location == 5) {
                    group.sendMessage(
                        String.format(
                            "得签着:%s(%s),奖励%s金币",
                            member.nick,
                            member.id,
                            lotteryInfo.bonus
                        )
                    )
                }
            }
        }

        /**
         * 关闭定时器
         */
        @JvmStatic
        fun close() {
            CronUtil.stop()
        }
    }
}
