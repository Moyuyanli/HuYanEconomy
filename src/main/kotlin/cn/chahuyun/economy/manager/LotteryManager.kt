package cn.chahuyun.economy.manager

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.LotteryInfoDto
import cn.chahuyun.economy.scheduler.HuYanScheduler
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MoneyFormatUtil
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.NormalMember
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 褰╃エ妯″潡鍒濆鍖栦笌瀹氭椂璋冨害绠＄悊銆?
 */
object LotteryManager {

    @JvmField
    val minuteTiming = AtomicBoolean(false)

    @JvmField
    val hoursTiming = AtomicBoolean(false)

    /**
     * 鍒濆鍖栧僵绁細妫€鏌ュ瓨閲忚褰曞苟鎸夐渶鍚姩瀹氭椂浠诲姟銆?
     */
    @JvmStatic
    fun init() {
        val lotteryInfos = try {
            findAll()
        } catch (e: Exception) {
            Log.error("褰╃エ绠＄悊:褰╃エ鍒濆鍖栧け璐?", e)
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
     * 鍙戦€佸僵绁ㄧ粨鏋滀俊鎭€?     */
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
            runBlocking { member.sendMessage("濂栭噾娣诲姞澶辫触锛岃鑱旂郴绠＄悊鍛?") }
            return
        }

        runBlocking { member.sendMessage(lotteryInfo.toMessage()) }
        when (type) {
            1 -> if (location == 3) {
                runBlocking {
                    group.sendMessage(
                        "寰楃鐫€:${member.nick}(${member.id}),濂栧姳${MoneyFormatUtil.format(lotteryInfo.bonus)}閲戝竵"
                    )
                }
            }

            2 -> if (location == 4) {
                runBlocking {
                    group.sendMessage(
                        "寰楃鐫€:${member.nick}(${member.id}),濂栧姳${MoneyFormatUtil.format(lotteryInfo.bonus)}閲戝竵"
                    )
                }
            }

            3 -> if (location == 5) {
                runBlocking {
                    group.sendMessage(
                        "寰楃鐫€:${member.nick}(${member.id}),濂栧姳${MoneyFormatUtil.format(lotteryInfo.bonus)}閲戝竵"
                    )
                }
            }
        }
    }

    /**
     * 鍏抽棴瀹氭椂鍣ㄣ€?
     */
    @JvmStatic
    fun close() {
        HuYanScheduler.cancel("dayTask")
        HuYanScheduler.cancel("hoursTask")
        HuYanScheduler.cancel("minutesTask")
        minuteTiming.set(false)
        hoursTiming.set(false)
    }

    fun findByType(type: Int): List<LotteryInfoDto> = lotteryProxy.findWhere { it.type == type }

    fun findAll(): List<LotteryInfoDto> = lotteryProxy.findAll()

    fun save(lotteryInfo: LotteryInfoDto): LotteryInfoDto = lotteryProxy.save(lotteryInfo)

    fun delete(lotteryInfo: LotteryInfoDto): Boolean = lotteryProxy.delete(lotteryInfo.id.toLong())

    private val lotteryProxy
        get() = EntityProxyRegistry.get<LotteryInfoDto>("lottery") ?: error("彩票代理器未初始化")
}
