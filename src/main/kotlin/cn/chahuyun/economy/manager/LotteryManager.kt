package cn.chahuyun.economy.manager

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.entity.LotteryInfo
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MoneyFormatUtil
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.cron.CronUtil
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.NormalMember
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 彩票模块初始化与定时调度管理。
 */
object LotteryManager {

    @JvmField
    val minuteTiming = AtomicBoolean(false)

    @JvmField
    val hoursTiming = AtomicBoolean(false)

    /**
     * 初始化彩票：检查存量记录并按需启动定时任务。
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
                    val key = lotteryInfo.number ?: continue
                    minutesLottery[key] = lotteryInfo
                    continue
                }

                2 -> {
                    val key = lotteryInfo.number ?: continue
                    hoursLottery[key] = lotteryInfo
                    continue
                }

                3 -> {
                    val key = lotteryInfo.number ?: continue
                    dayLottery[key] = lotteryInfo
                }
            }
        }

        if (minutesLottery.isNotEmpty()) {
            ensureMinutesSchedule()
        }

        if (hoursLottery.isNotEmpty()) {
            ensureHoursSchedule()
        }

        if (dayLottery.isNotEmpty()) {
            val dayTaskId = "dayTask"
            CronUtil.remove(dayTaskId)
            val dayTask = LotteryDayTask(dayTaskId)
            CronUtil.schedule(dayTaskId, "0 0 0 * * ?", dayTask)
        }
    }

    @JvmStatic
    fun ensureHoursSchedule() {
        if (hoursTiming.get()) {
            return
        }
        val hoursTaskId = "hoursTask"
        CronUtil.remove(hoursTaskId)
        val hoursTask = LotteryHoursTask(hoursTaskId)
        CronUtil.schedule(hoursTaskId, "0 0 * * * ?", hoursTask)
        hoursTiming.set(true)
    }

    @JvmStatic
    fun ensureMinutesSchedule() {
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
     * 发送彩票结果信息。
     */
    @JvmStatic
    fun result(type: Int, location: Int, lotteryInfo: LotteryInfo) {
        if (location == 0) {
            lotteryInfo.remove()
            return
        }
        val bot: Bot = HuYanEconomy.bot ?: return
        val group = bot.getGroup(lotteryInfo.group) ?: return
        val member: NormalMember = group.get(lotteryInfo.qq) ?: return
        lotteryInfo.remove()

        if (!EconomyUtil.plusMoneyToUser(member, lotteryInfo.bonus)) {
            runBlocking { member.sendMessage("奖金添加失败，请联系管理员!") }
            return
        }

        runBlocking { member.sendMessage(lotteryInfo.toMessage()) }
        when (type) {
            1 -> if (location == 3) {
                runBlocking {
                    group.sendMessage(
                        "得签着:${member.nick}(${member.id}),奖励${MoneyFormatUtil.format(lotteryInfo.bonus)}金币"
                    )
                }
            }

            2 -> if (location == 4) {
                runBlocking {
                    group.sendMessage(
                        "得签着:${member.nick}(${member.id}),奖励${MoneyFormatUtil.format(lotteryInfo.bonus)}金币"
                    )
                }
            }

            3 -> if (location == 5) {
                runBlocking {
                    group.sendMessage(
                        "得签着:${member.nick}(${member.id}),奖励${MoneyFormatUtil.format(lotteryInfo.bonus)}金币"
                    )
                }
            }
        }
    }

    /**
     * 关闭定时器（沿用旧行为：停止 CronUtil）。
     */
    @JvmStatic
    fun close() {
        CronUtil.stop()
    }
}
