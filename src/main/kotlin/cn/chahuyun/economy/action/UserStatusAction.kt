package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.constant.MessageConversionEnum
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.constant.UserLocation
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.entity.UserStatus
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.ShareUtils
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain
import java.util.*

/**
 * 用户状态管理
 */
@EventComponent
class UserStatusAction {

    @MessageAuthorize(text = ["我的状态", "我的位置"])
    suspend fun myStatus(event: GroupMessageEvent) {
        val sender: Member = event.sender
        val message: MessageChain = event.message
        val group: Group = event.group

        val userInfo: UserInfo = UserCoreManager.getUserInfo(sender)
        val userStatus: UserStatus = getUserStatus(userInfo)

        when (userStatus.place) {
            UserLocation.HOME -> {
                group.sendMessage(MessageUtil.formatMessageChain(message, "你现在正在家里躺着哩~"))
                return
            }

            UserLocation.PRISON -> {
                val time = userStatus.recoveryTime
                val between = DateUtil.between(userStatus.startTime, Date(), DateUnit.MINUTE)
                group.sendMessage(MessageUtil.formatMessageChain(message, "你还在监狱，剩余拘禁时间:%s分钟", time - between))
                return
            }

            UserLocation.HOSPITAL -> {
                group.sendMessage(MessageUtil.formatMessageChain(message, "你现在正在医院躺着，wifi速度还行."))
                return
            }

            UserLocation.FACTORY -> {
                group.sendMessage(MessageUtil.formatMessageChain(message, "你现在正在工厂，这里很吵闹!"))
                return
            }

            UserLocation.FISHPOND -> {
                group.sendMessage(MessageUtil.formatMessageChain(message, "嘘，别把我的鱼吓跑了!"))
                return
            }

            else -> {}
        }
    }

    @MessageAuthorize(text = ["回家"])
    suspend fun goHome(event: GroupMessageEvent) {
        val group: Group = event.group
        val message: MessageChain = event.message
        val sender: Member = event.sender

        val userInfo: UserInfo = UserCoreManager.getUserInfo(sender)

        if (checkUserInHome(userInfo)) {
            group.sendMessage(MessageUtil.formatMessageChain(message, "你已经在家里了！"))
            return
        }

        if (checkUserInHospital(userInfo) || checkUserInPrison(userInfo)) {
            group.sendMessage(MessageUtil.formatMessageChain(message, "你现在还不能回家！"))
        } else {
            moveHome(userInfo)
            group.sendMessage(MessageUtil.formatMessageChain(message, "你回家躺着去."))
        }
    }

    @MessageAuthorize(
        text = ["回家 ?@\\d+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        messageConversion = MessageConversionEnum.CONTENT,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun gotoHome(event: GroupMessageEvent) {
        val group: Group = event.group
        val message: MessageChain = event.message

        val member: Member? = ShareUtils.getAtMember(event)
        if (member == null) {
            Log.warning("该用户不存在!")
            return
        }

        val userInfo: UserInfo = UserCoreManager.getUserInfo(member)
        moveHome(userInfo)
        group.sendMessage(MessageUtil.formatMessageChain(message, "你让ta回家躺着去."))
    }

    companion object {
        /**
         * 检查用户是否在家
         */
        @JvmStatic
        fun checkUserInHome(user: UserInfo): Boolean {
            val userStatus = getUserStatus(user.qq)
            return userStatus.place == UserLocation.HOME
        }

        /**
         * 检查用户是否不在家
         */
        @JvmStatic
        fun checkUserNotInHome(user: UserInfo): Boolean {
            val userStatus = getUserStatus(user.qq)
            return userStatus.place != UserLocation.HOME
        }

        /**
         * 回家
         */
        @JvmStatic
        fun moveHome(user: UserInfo) {
            val userStatus = getUserStatus(user.qq)
            userStatus.place = UserLocation.HOME
            userStatus.recoveryTime = 0
            userStatus.startTime = Date()
            HibernateFactory.merge(userStatus)
        }

        /**
         * 检查用户是否在医院
         */
        @JvmStatic
        fun checkUserInHospital(user: UserInfo): Boolean {
            val userStatus = getUserStatus(user.qq)
            return userStatus.place == UserLocation.HOSPITAL
        }

        /**
         * 进医院咯~~
         */
        @JvmStatic
        fun moveHospital(user: UserInfo, recovery: Int) {
            val userStatus = getUserStatus(user.qq)
            userStatus.place = UserLocation.HOSPITAL
            userStatus.recoveryTime = recovery
            userStatus.startTime = Date()
            HibernateFactory.merge(userStatus)
        }

        /**
         * 检查用户是否在监狱
         */
        @JvmStatic
        fun checkUserInPrison(user: UserInfo): Boolean {
            val userStatus = getUserStatus(user.qq)
            return userStatus.place == UserLocation.PRISON
        }

        /**
         * 蹲大牢咯~~
         */
        @JvmStatic
        fun movePrison(user: UserInfo, recovery: Int) {
            val userStatus = getUserStatus(user.qq)
            userStatus.place = UserLocation.PRISON
            userStatus.recoveryTime = recovery
            userStatus.startTime = Date()
            HibernateFactory.merge(userStatus)
        }

        /**
         * 检查用户是否在鱼塘
         */
        @JvmStatic
        fun checkUserInFishpond(user: UserInfo): Boolean {
            val userStatus = getUserStatus(user.qq)
            return userStatus.place == UserLocation.FISHPOND
        }

        /**
         * 钓鱼去~~
         */
        @JvmStatic
        fun moveFishpond(user: UserInfo, recovery: Int) {
            val userStatus = getUserStatus(user.qq)
            userStatus.place = UserLocation.FISHPOND
            userStatus.recoveryTime = recovery
            userStatus.startTime = Date()
            HibernateFactory.merge(userStatus)
        }

        /**
         * 检查用户是否在工厂
         */
        @JvmStatic
        fun checkUserInFactory(user: UserInfo): Boolean {
            val userStatus = getUserStatus(user.qq)
            return userStatus.place == UserLocation.HOME
        }

        /**
         * 进厂子~~
         */
        @JvmStatic
        fun moveFactory(user: UserInfo, recovery: Int) {
            val userStatus = getUserStatus(user.qq)
            userStatus.place = UserLocation.FACTORY
            userStatus.recoveryTime = recovery
            userStatus.startTime = Date()
            HibernateFactory.merge(userStatus)
        }

        /**
         * 获取用户状态
         */
        @JvmStatic
        fun getUserStatus(user: UserInfo): UserStatus {
            return getUserStatus(user.qq)
        }

        /**
         * 获取用户状态
         */
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
}
