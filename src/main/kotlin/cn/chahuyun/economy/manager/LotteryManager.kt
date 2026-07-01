package cn.chahuyun.economy.manager

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.model.LotteryInfoDto
import cn.chahuyun.economy.proxy.EntityProxyRegistry
import cn.chahuyun.economy.scheduler.HuYanScheduler
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MoneyFormatUtil
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
            findAll()
        } catch (e: Exception) {
            Log.error("彩票管理:彩票初始化失败!", e)
            return
        }

        val minutesLottery = HashMap<String, LotteryInfoDto>()
        val hoursLottery = HashMap<String, LotteryInfoDto>()
        val dayLottery = HashMap<String, LotteryInfoDto>()

        for (lotteryInfo in lotteryInfos) {
            when (lotteryInfo.type) {
                1 -> {
                    val key = lotteryInfo.number.takeIf { it.isNotBlank() } ?: continue
                    minutesLottery[key] = lotteryInfo
                    continue
                }

                2 -> {
                    val key = lotteryInfo.number.takeIf { it.isNotBlank() } ?: continue
                    hoursLottery[key] = lotteryInfo
                    continue
                }

                3 -> {
                    val key = lotteryInfo.number.takeIf { it.isNotBlank() } ?: continue
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
            HuYanScheduler.cancel(dayTaskId)
            val dayTask = LotteryDayTask(dayTaskId)
            HuYanScheduler.schedule(dayTaskId, "0 0 0 * * ?", dayTask)
        }
    }

    @JvmStatic
    fun ensureHoursSchedule() {
        if (hoursTiming.get()) {
            return
        }
        val hoursTaskId = "hoursTask"
        HuYanScheduler.cancel(hoursTaskId)
        val hoursTask = LotteryHoursTask(hoursTaskId)
        HuYanScheduler.schedule(hoursTaskId, "0 0 * * * ?", hoursTask)
        hoursTiming.set(true)
    }

    @JvmStatic
    fun ensureMinutesSchedule() {
        if (minuteTiming.get()) {
            return
        }
        val minutesTaskId = "minutesTask"
        HuYanScheduler.cancel(minutesTaskId)
        val minutesTask = LotteryMinutesTask(minutesTaskId)
        HuYanScheduler.schedule(minutesTaskId, "0 * * * * ?", minutesTask)
        minuteTiming.set(true)
    }

    /**
     * 发送彩票结果信息。
     */
    @JvmStatic
    fun result(type: Int, location: Int, lotteryInfo: LotteryInfoDto) {
        if (location == 0) {
            delete(lotteryInfo)
            return
        }
        val bot: Bot = HuYanEconomy.bot ?: return
        val group = bot.getGroup(lotteryInfo.group) ?: return
        val member: NormalMember = group.get(lotteryInfo.qq) ?: return
        delete(lotteryInfo)

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
     * 关闭定时器。
     */
    @JvmStatic
    fun close() {
        HuYanScheduler.stop()
    }

    fun findByType(type: Int): List<LotteryInfoDto> = lotteryProxy.findWhere { it.type == type }

    fun findAll(): List<LotteryInfoDto> = lotteryProxy.findAll()

    fun save(lotteryInfo: LotteryInfoDto): LotteryInfoDto = lotteryProxy.save(lotteryInfo)

    fun delete(lotteryInfo: LotteryInfoDto): Boolean = lotteryProxy.delete(lotteryInfo.id.toLong())

    private val lotteryProxy
        get() = EntityProxyRegistry.get<LotteryInfoDto>("lottery") ?: error("彩票代理器未初始化")
}
