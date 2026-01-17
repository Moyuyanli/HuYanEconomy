package cn.chahuyun.economy.manager

import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.entity.rob.RobInfo
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object RobManager {

    private val cooling: MutableMap<Long, Date> = ConcurrentHashMap()

    fun getCoolingRemainingMinutes(qq: Long, cooldownMinutes: Long): Long {
        val last = cooling[qq] ?: return 0
        val between = DateUtil.between(last, Date(), DateUnit.MINUTE, true)
        return if (between < cooldownMinutes) cooldownMinutes - between else 0
    }

    fun markCooling(qq: Long) {
        cooling[qq] = Date()
    }

    /**
     * 获取抢劫信息
     */
    @JvmStatic
    fun getRobInfo(userInfo: UserInfo): RobInfo {
        var one = HibernateFactory.selectOneById(RobInfo::class.java, userInfo.qq)
        if (one == null) {
            one = RobInfo(userInfo.qq, Date(), 0, 0, 0)
            return HibernateFactory.merge(one)
        }
        return one
    }
}
