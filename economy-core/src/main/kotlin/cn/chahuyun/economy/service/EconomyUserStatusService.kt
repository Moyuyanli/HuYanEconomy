package cn.chahuyun.economy.service

import cn.chahuyun.economy.manager.UserStatusManager
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.model.user.UserStatusDto

/**
 * Core-facing user status operations for feature modules.
 */
object EconomyUserStatusService {

    @JvmStatic
    fun getStatus(userInfo: UserInfoDto): UserStatusDto =
        UserStatusManager.getUserStatus(userInfo)

    @JvmStatic
    fun getStatus(userId: Long): UserStatusDto =
        UserStatusManager.getUserStatus(userId)

    @JvmStatic
    fun isNotHome(userInfo: UserInfoDto): Boolean =
        UserStatusManager.checkUserNotInHome(userInfo)

    @JvmStatic
    fun isInHospital(userInfo: UserInfoDto): Boolean =
        UserStatusManager.checkUserInHospital(userInfo)

    @JvmStatic
    fun isInPrison(userInfo: UserInfoDto): Boolean =
        UserStatusManager.checkUserInPrison(userInfo)

    @JvmStatic
    fun isInFishpond(userInfo: UserInfoDto): Boolean =
        UserStatusManager.checkUserInFishpond(userInfo)

    @JvmStatic
    fun moveHome(userInfo: UserInfoDto) =
        UserStatusManager.moveHome(userInfo)

    @JvmStatic
    fun moveHospital(userInfo: UserInfoDto, recovery: Int) =
        UserStatusManager.moveHospital(userInfo, recovery)

    @JvmStatic
    fun movePrison(userInfo: UserInfoDto, recovery: Int) =
        UserStatusManager.movePrison(userInfo, recovery)

    @JvmStatic
    fun moveFishpond(userInfo: UserInfoDto, recovery: Int) =
        UserStatusManager.moveFishpond(userInfo, recovery)
}
