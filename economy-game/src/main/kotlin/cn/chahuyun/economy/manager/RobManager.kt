package cn.chahuyun.economy.manager

import cn.chahuyun.economy.model.rob.RobInfoDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.service.RobService

object RobManager {
    fun getCoolingRemainingMinutes(qq: Long, cooldownMinutes: Long): Long =
        RobService.getCoolingRemainingMinutes(qq, cooldownMinutes)

    fun markCooling(qq: Long) {
        RobService.markCooling(qq)
    }

    /**
     * 获取抢劫信息。
     */
    @JvmStatic
    fun getRobInfo(userInfo: UserInfoDto): RobInfoDto =
        RobService.getRobInfo(userInfo)

    fun saveRobInfo(robInfo: RobInfoDto): RobInfoDto =
        RobService.saveRobInfo(robInfo)
}
