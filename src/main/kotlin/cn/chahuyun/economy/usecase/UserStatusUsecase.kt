package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.constant.UserLocation
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.entity.UserStatus
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.manager.UserStatusManager
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.ShareUtils
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain
import java.util.*

/**
 * 用户状态相关用例。
 */
object UserStatusUsecase {

    suspend fun myStatus(event: GroupMessageEvent) {
        val sender: Member = event.sender
        val message: MessageChain = event.message
        val group: Group = event.group

        val userInfo: UserInfo = UserCoreManager.getUserInfo(sender)
        val userStatus: UserStatus = UserStatusManager.getUserStatus(userInfo)

        when (userStatus.place) {
            UserLocation.HOME -> {
                group.sendMessage(MessageUtil.formatMessageChain(message, "你现在正在家里躺着哩~"))
                return
            }

            UserLocation.PRISON -> {
                val time = userStatus.recoveryTime
                val between = DateUtil.between(userStatus.startTime, Date(), DateUnit.MINUTE)
                group.sendMessage(
                    MessageUtil.formatMessageChain(message, "你还在监狱，剩余拘禁时间:${time - between}分钟")
                )
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

    suspend fun goHome(event: GroupMessageEvent) {
        val group: Group = event.group
        val message: MessageChain = event.message
        val sender: Member = event.sender

        val userInfo: UserInfo = UserCoreManager.getUserInfo(sender)

        if (UserStatusManager.checkUserInHome(userInfo)) {
            group.sendMessage(MessageUtil.formatMessageChain(message, "你已经在家里了！"))
            return
        }

        if (UserStatusManager.checkUserInHospital(userInfo) || UserStatusManager.checkUserInPrison(userInfo)) {
            group.sendMessage(MessageUtil.formatMessageChain(message, "你现在还不能回家！"))
        } else {
            UserStatusManager.moveHome(userInfo)
            group.sendMessage(MessageUtil.formatMessageChain(message, "你回家躺着去."))
        }
    }

    suspend fun gotoHome(event: GroupMessageEvent) {
        val group: Group = event.group
        val message: MessageChain = event.message

        val member: Member? = ShareUtils.getAtMember(event)
        if (member == null) {
            Log.warning("该用户不存在!")
            return
        }

        val userInfo: UserInfo = UserCoreManager.getUserInfo(member)
        UserStatusManager.moveHome(userInfo)
        group.sendMessage(MessageUtil.formatMessageChain(message, "你让ta回家躺着去."))
    }
}
