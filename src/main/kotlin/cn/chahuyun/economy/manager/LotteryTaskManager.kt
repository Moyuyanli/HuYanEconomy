package cn.chahuyun.economy.manager

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.entity.LotteryInfo
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.util.RandomUtil
import cn.hutool.cron.CronUtil
import cn.hutool.cron.task.Task
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.NormalMember

/**
 * 彩票定时任务（从 `LotteryAction.java` 迁移）。
 * - 分钟：小签
 * - 小时：中签
 * - 天：大签
 */

class LotteryMinutesTask(
    private val id: String
) : Task {

    override fun execute(): Unit = runBlocking {
        val bot = requireNotNull(HuYanEconomy.bot)
        val current = arrayOf(
            RandomUtil.randomInt(0, 9).toString(),
            RandomUtil.randomInt(0, 9).toString(),
            RandomUtil.randomInt(0, 9).toString()
        )

        val currentString = buildString {
            append(current[0])
            for (i in 1 until current.size) {
                append(",").append(current[i])
            }
        }

        val groups = HashSet<Long>()

        val lotteryInfoList = HibernateFactory.selectList(LotteryInfo::class.java, "type", 1)
        if (lotteryInfoList.isEmpty()) {
            return@runBlocking
        }

        for (lotteryInfo in lotteryInfoList) {
            groups.add(lotteryInfo.group)
            var location = 0
            var bonus = 0.0

            val split = lotteryInfo.number!!.split(",")
            for (i in split.indices) {
                if (split[i] == current[i]) {
                    location++
                }
            }

            when (location) {
                3 -> bonus = lotteryInfo.money * 160
                2 -> bonus = lotteryInfo.money * 6
                1 -> bonus = lotteryInfo.money * 0.7
            }

            lotteryInfo.bonus = bonus
            lotteryInfo.current = currentString
            val merged = HibernateFactory.merge(lotteryInfo)
            LotteryManager.result(1, location, merged)
        }

        for (group in groups) {
            val format = "本期小签开签啦！\n开签号码$currentString"
            bot.getGroup(group)?.sendMessage(format)
        }

        CronUtil.remove(id)
        LotteryManager.minuteTiming.set(false)
    }
}

class LotteryHoursTask(
    private val id: String
) : Task {

    override fun execute(): Unit = runBlocking {
        val bot = requireNotNull(HuYanEconomy.bot)
        val current = arrayOf(
            RandomUtil.randomInt(0, 10).toString(),
            RandomUtil.randomInt(0, 10).toString(),
            RandomUtil.randomInt(0, 10).toString(),
            RandomUtil.randomInt(0, 10).toString()
        )

        val currentString = buildString {
            append(current[0])
            for (i in 1 until current.size) {
                append(",").append(current[i])
            }
        }

        val groups = HashSet<Long>()

        val lotteryInfos = HibernateFactory.selectList(LotteryInfo::class.java, "type", 2)
        if (lotteryInfos.isEmpty()) {
            return@runBlocking
        }

        for (lotteryInfo in lotteryInfos) {
            groups.add(lotteryInfo.group)
            var location = 0
            var bonus = 0.0

            val split = lotteryInfo.number!!.split(",")
            for (i in split.indices) {
                if (split[i] == current[i]) {
                    location++
                }
            }

            when (location) {
                4 -> bonus = lotteryInfo.money * 1250
                3 -> bonus = lotteryInfo.money * 35
                2 -> bonus = lotteryInfo.money * 2.5
                1 -> bonus = lotteryInfo.money * 0.5
            }

            lotteryInfo.bonus = bonus
            lotteryInfo.current = currentString
            val merged = HibernateFactory.merge(lotteryInfo)
            LotteryManager.result(2, location, merged)
        }

        for (group in groups) {
            val format = "本期中签开签啦！\n开签号码$currentString"
            bot.getGroup(group)?.sendMessage(format)
        }

        CronUtil.remove(id)
        LotteryManager.hoursTiming.set(false)
    }
}

class LotteryDayTask(
    private val id: String
) : Task {

    override fun execute(): Unit = runBlocking {
        val bot = requireNotNull(HuYanEconomy.bot)
        val current = arrayOf(
            RandomUtil.randomInt(0, 10).toString(),
            RandomUtil.randomInt(0, 10).toString(),
            RandomUtil.randomInt(0, 10).toString(),
            RandomUtil.randomInt(0, 10).toString(),
            RandomUtil.randomInt(0, 10).toString()
        )

        val currentString = buildString {
            append(current[0])
            for (i in 1 until current.size) {
                append(",").append(current[i])
            }
        }

        val groups = HashSet<Long>()
        val list = ArrayList<LotteryInfo>()

        // NOTE: 这里沿用原实现（type=2），保持行为不变
        val lotteryInfos = HibernateFactory.selectList(LotteryInfo::class.java, "type", 2)
        if (lotteryInfos.isEmpty()) {
            return@runBlocking
        }

        for (lotteryInfo in lotteryInfos) {
            groups.add(lotteryInfo.group)
            var location = 0
            var bonus = 0.0

            val split = lotteryInfo.number!!.split(",")
            for (i in split.indices) {
                if (split[i] == current[i]) {
                    location++
                }
            }

            when (location) {
                5 -> bonus = lotteryInfo.money * 10000
                4 -> bonus = lotteryInfo.money * 200
                3 -> bonus = lotteryInfo.money * 12
                2 -> bonus = lotteryInfo.money * 1.4
                1 -> bonus = lotteryInfo.money * 0.3
            }

            lotteryInfo.bonus = bonus
            lotteryInfo.current = currentString
            val merged = HibernateFactory.merge(lotteryInfo)
            LotteryManager.result(3, location, merged)
            if (location == 5) {
                list.add(merged)
            }
        }

        for (group in groups) {
            val botGroup: Group? = bot.getGroup(group)
            val format = "本期大签开签啦！\n开签号码$currentString"

            // NOTE: 这里沿用原实现：只发送 format，不发送获奖列表（保持行为不变）
            // 原实现里组装了消息但未发送。
            @Suppress("UNUSED_VARIABLE")
            val ignored = list.map { lotteryInfo ->
                val normalMember: NormalMember? = botGroup?.get(lotteryInfo.qq)
                if (normalMember == null) {
                    "${lotteryInfo.qq}:${lotteryInfo.number}->奖金:${lotteryInfo.bonus}"
                } else {
                    "${normalMember.nick}:${lotteryInfo.number}->奖金:${lotteryInfo.bonus}"
                }
            }

            botGroup?.sendMessage(format)
        }

        CronUtil.remove(id)
    }
}


