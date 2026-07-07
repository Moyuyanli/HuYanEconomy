package cn.chahuyun.economy.service

import cn.chahuyun.economy.manager.LotteryManager
import cn.chahuyun.economy.model.LotteryInfoDto

object LotteryService {

    fun save(lotteryInfo: LotteryInfoDto): LotteryInfoDto =
        LotteryDataService.save(lotteryInfo)

    fun ensureSchedule(type: Int) {
        when (type) {
            1 -> LotteryManager.ensureMinutesSchedule()
            2 -> LotteryManager.ensureHoursSchedule()
        }
    }
}
