package cn.chahuyun.economy.manager

import cn.chahuyun.economy.constant.UserLocation
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.entity.UserStatus
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import java.util.*

/**
 * 用户状态对外 API（从 UserStatusAction 迁移）。
 */
object UserStatusManager {

    @JvmStatic
    fun checkUserInHome(user: UserInfo): Boolean {
        val userStatus = getUserStatus(user.qq)
        return userStatus.place == UserLocation.HOME
    }

    @JvmStatic
    fun checkUserNotInHome(user: UserInfo): Boolean {
        val userStatus = getUserStatus(user.qq)
        return userStatus.place != UserLocation.HOME
    }

    @JvmStatic
    fun moveHome(user: UserInfo) {
        val userStatus = getUserStatus(user.qq)
        userStatus.place = UserLocation.HOME
        userStatus.recoveryTime = 0
        userStatus.startTime = Date()
        HibernateFactory.merge(userStatus)
    }

    @JvmStatic
    fun checkUserInHospital(user: UserInfo): Boolean {
        val userStatus = getUserStatus(user.qq)
        return userStatus.place == UserLocation.HOSPITAL
    }

    @JvmStatic
    fun moveHospital(user: UserInfo, recovery: Int) {
        val userStatus = getUserStatus(user.qq)
        userStatus.place = UserLocation.HOSPITAL
        userStatus.recoveryTime = recovery
        userStatus.startTime = Date()
        HibernateFactory.merge(userStatus)
    }

    @JvmStatic
    fun checkUserInPrison(user: UserInfo): Boolean {
        val userStatus = getUserStatus(user.qq)
        return userStatus.place == UserLocation.PRISON
    }

    @JvmStatic
    fun movePrison(user: UserInfo, recovery: Int) {
        val userStatus = getUserStatus(user.qq)
        userStatus.place = UserLocation.PRISON
        userStatus.recoveryTime = recovery
        userStatus.startTime = Date()
        HibernateFactory.merge(userStatus)
    }

    @JvmStatic
    fun checkUserInFishpond(user: UserInfo): Boolean {
        val userStatus = getUserStatus(user.qq)
        return userStatus.place == UserLocation.FISHPOND
    }

    @JvmStatic
    fun moveFishpond(user: UserInfo, recovery: Int) {
        val userStatus = getUserStatus(user.qq)
        userStatus.place = UserLocation.FISHPOND
        userStatus.recoveryTime = recovery
        userStatus.startTime = Date()
        HibernateFactory.merge(userStatus)
    }

    @JvmStatic
    fun checkUserInFactory(user: UserInfo): Boolean {
        val userStatus = getUserStatus(user.qq)
        return userStatus.place == UserLocation.HOME
    }

    @JvmStatic
    fun moveFactory(user: UserInfo, recovery: Int) {
        val userStatus = getUserStatus(user.qq)
        userStatus.place = UserLocation.FACTORY
        userStatus.recoveryTime = recovery
        userStatus.startTime = Date()
        HibernateFactory.merge(userStatus)
    }

    @JvmStatic
    fun getUserStatus(user: UserInfo): UserStatus {
        return getUserStatus(user.qq)
    }

    @JvmStatic
    fun getUserStatus(qq: Long): UserStatus {
        var one = HibernateFactory.selectOneById(UserStatus::class.java, qq)

        if (one == null) {
            val status = UserStatus()
            status.id = qq
            return HibernateFactory.merge(status)
        }

        val time = one.recoveryTime
        if (time != 0 && one.place != UserLocation.HOSPITAL) {
            val startTime = one.startTime
            val between = DateUtil.between(startTime, Date(), DateUnit.MINUTE, true)
            if (between > time) {
                one.recoveryTime = 0
                one.place = UserLocation.HOME
                return HibernateFactory.merge(one)
            }
        }

        return one
    }
}
