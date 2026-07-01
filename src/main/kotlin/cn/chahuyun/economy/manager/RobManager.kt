package cn.chahuyun.economy.manager

import cn.chahuyun.economy.model.rob.RobInfoDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.proxy.EntityProxyRegistry
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
    fun getRobInfo(userInfo: UserInfoDto): RobInfoDto {
        return robProxy.findById(userInfo.qq)
            ?: robProxy.save(RobInfoDto(userId = userInfo.qq, nowTime = Date().time))
    }

    fun saveRobInfo(robInfo: RobInfoDto): RobInfoDto = robProxy.save(robInfo)

    private val robProxy
        get() = EntityProxyRegistry.get<RobInfoDto>("rob") ?: error("抢劫代理器未初始化")
}
