package cn.chahuyun.economy.service

import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.utils.Log
import cn.hutool.core.date.CalendarUtil
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import java.util.*

object UserSignRuleService {

    fun applySign(dto: UserInfoDto, reSignTime: Int): Boolean {
        if (dto.signTime == 0L) {
            dto.sign = true
            dto.signTime = Date().time
            dto.signNumber = 1
            return true
        }

        val calendar = CalendarUtil.calendar(DateUtil.offsetDay(Date(dto.signTime), 1))
        calendar.set(java.util.Calendar.HOUR_OF_DAY, reSignTime)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        val time = calendar.time
        val between = DateUtil.between(time, Date(), DateUnit.MINUTE, false)
        Log.debug("用户:(${dto.qq})签到时间差->$between")
        if (between < 0) {
            return false
        } else if (between <= 1440) {
            dto.signNumber += 1
            if (dto.signNumber == 2) {
                dto.oldSignNumber = 0
            }
        } else {
            dto.oldSignNumber = dto.signNumber
            dto.signNumber = 1
        }
        dto.sign = true
        dto.signTime = Date().time
        return true
    }
}
