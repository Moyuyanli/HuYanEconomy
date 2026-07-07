package cn.chahuyun.economy.manager

import cn.chahuyun.economy.constant.UserLocation
import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.model.user.UserStatusDto
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import java.util.*

/**
 * 用户状态对外 API。
 */
object UserStatusManager {

    /**
     * 通过代理器获取用户状态 DTO。
     */
    fun getUserStatusDto(id: Long): UserStatusDto? {
        return statusProxy.findById(id)
    }

    /**
     * 通过代理器保存用户状态。
     */
    fun saveUserStatusDto(dto: UserStatusDto): UserStatusDto {
        return statusProxy.save(dto)
    }

    @JvmStatic
    fun checkUserInHome(user: UserInfoDto): Boolean {
        val userStatus = getUserStatus(user.qq)
        return userStatus.place == UserLocation.HOME.name
    }

    @JvmStatic
    fun checkUserNotInHome(user: UserInfoDto): Boolean {
        val userStatus = getUserStatus(user.qq)
        return userStatus.place != UserLocation.HOME.name
    }

    @JvmStatic
    fun moveHome(user: UserInfoDto) {
        val userStatus = getUserStatus(user.qq)
        saveUserStatusDto(userStatus.copy(place = UserLocation.HOME.name, recoveryTime = 0, startTime = Date().time))
    }

    @JvmStatic
    fun checkUserInHospital(user: UserInfoDto): Boolean {
        val userStatus = getUserStatus(user.qq)
        return userStatus.place == UserLocation.HOSPITAL.name
    }

    @JvmStatic
    fun moveHospital(user: UserInfoDto, recovery: Int) {
        val userStatus = getUserStatus(user.qq)
        saveUserStatusDto(userStatus.copy(place = UserLocation.HOSPITAL.name, recoveryTime = recovery, startTime = Date().time))
    }

    @JvmStatic
    fun checkUserInPrison(user: UserInfoDto): Boolean {
        val userStatus = getUserStatus(user.qq)
        return userStatus.place == UserLocation.PRISON.name
    }

    @JvmStatic
    fun movePrison(user: UserInfoDto, recovery: Int) {
        val userStatus = getUserStatus(user.qq)
        saveUserStatusDto(userStatus.copy(place = UserLocation.PRISON.name, recoveryTime = recovery, startTime = Date().time))
    }

    @JvmStatic
    fun checkUserInFishpond(user: UserInfoDto): Boolean {
        val userStatus = getUserStatus(user.qq)
        return userStatus.place == UserLocation.FISHPOND.name
    }

    @JvmStatic
    fun moveFishpond(user: UserInfoDto, recovery: Int) {
        val userStatus = getUserStatus(user.qq)
        saveUserStatusDto(userStatus.copy(place = UserLocation.FISHPOND.name, recoveryTime = recovery, startTime = Date().time))
    }

    @JvmStatic
    fun checkUserInFactory(user: UserInfoDto): Boolean {
        val userStatus = getUserStatus(user.qq)
        return userStatus.place == UserLocation.FACTORY.name
    }

    @JvmStatic
    fun moveFactory(user: UserInfoDto, recovery: Int) {
        val userStatus = getUserStatus(user.qq)
        saveUserStatusDto(userStatus.copy(place = UserLocation.FACTORY.name, recoveryTime = recovery, startTime = Date().time))
    }

    @JvmStatic
    fun getUserStatus(user: UserInfoDto): UserStatusDto {
        return getUserStatus(user.qq)
    }

    @JvmStatic
    fun getUserStatus(qq: Long): UserStatusDto {
        var one = getUserStatusDto(qq)

        if (one == null) {
            return saveUserStatusDto(UserStatusDto(id = qq, startTime = Date().time))
        }

        val time = one.recoveryTime
        if (time != 0 && one.place != UserLocation.HOSPITAL.name) {
            val startTime = Date(one.startTime)
            val between = DateUtil.between(startTime, Date(), DateUnit.MINUTE, true)
            if (between > time) {
                one = one.copy(recoveryTime = 0, place = UserLocation.HOME.name)
                return saveUserStatusDto(one)
            }
        }

        return one
    }

    private val statusProxy
        get() = EntityProxyRegistry.get<UserStatusDto>("user_status") ?: error("用户状态代理器未初始化")
}
