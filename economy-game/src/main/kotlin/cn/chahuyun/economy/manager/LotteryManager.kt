package cn.chahuyun.economy.manager

import cn.chahuyun.economy.model.LotteryInfoDto
import cn.chahuyun.economy.scheduler.HuYanScheduler
import cn.chahuyun.economy.service.LotteryDataService
import cn.chahuyun.economy.service.LotteryPayoutService
import cn.chahuyun.economy.utils.Log
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
            LotteryDataService.findAll()
        } catch (e: Exception) {
            Log.error("彩票管理:彩票初始化失败", e)
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
    fun result(type: Int, location: Int, lotteryInfo: LotteryInfoDto) =
        LotteryPayoutService.result(type, location, lotteryInfo)

    /**
     * 关闭定时器。
     */
    @JvmStatic
    fun close() {
        HuYanScheduler.cancel("dayTask")
        HuYanScheduler.cancel("hoursTask")
        HuYanScheduler.cancel("minutesTask")
        minuteTiming.set(false)
        hoursTiming.set(false)
    }

    fun findByType(type: Int): List<LotteryInfoDto> =
        LotteryDataService.findByType(type)

    fun findAll(): List<LotteryInfoDto> =
        LotteryDataService.findAll()

    fun save(lotteryInfo: LotteryInfoDto): LotteryInfoDto =
        LotteryDataService.save(lotteryInfo)

    fun delete(lotteryInfo: LotteryInfoDto): Boolean =
        LotteryDataService.delete(lotteryInfo)
}
