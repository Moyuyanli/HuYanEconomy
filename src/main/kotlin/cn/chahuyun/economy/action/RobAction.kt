package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.constant.MessageConversionEnum
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.constant.TitleCode
import cn.chahuyun.economy.entity.UserBackpack
import cn.chahuyun.economy.entity.UserFactor
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.entity.UserStatus
import cn.chahuyun.economy.entity.rob.RobInfo
import cn.chahuyun.economy.manager.BackpackManager
import cn.chahuyun.economy.manager.TitleManager
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.props.FunctionProps
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.plugin.FactorManager
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.ShareUtils
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain
import java.util.*

/**
 * 抢劫管理
 */
@EventComponent
class RobAction {

    @MessageAuthorize(
        text = ["抢劫 ?@?\\d{6,11} ?"],
        messageMatching = MessageMatchingEnum.REGULAR,
        messageConversion = MessageConversionEnum.CONTENT,
        groupPermissions = [EconPerm.ROB_PERM]
    )
    suspend fun rob(event: GroupMessageEvent) {
        Log.info("抢劫指令")

        val group: Group = event.group
        val message: MessageChain = event.message

        val user: Member = event.sender
        val thisUser: UserInfo = UserCoreManager.getUserInfo(user)

        if (cooling.containsKey(user)) {
            val date = cooling[user]
            if (date != null) {
                val between = DateUtil.between(date, Date(), DateUnit.MINUTE, true)
                if (between < 10) {
                    group.sendMessage(
                        MessageUtil.formatMessageChain(
                            message,
                            "再等等吧，你还得歇%d分钟才能行动!",
                            10 - between
                        )
                    )
                    return
                }
            }
        }

        cooling[user] = Date()

        if (UserStatusAction.checkUserNotInHome(thisUser)) {
            val userStatus: UserStatus = UserStatusAction.getUserStatus(user.id)
            when (userStatus.place) {
                cn.chahuyun.economy.constant.UserLocation.HOSPITAL -> {
                    group.sendMessage(MessageUtil.formatMessageChain(message, "你还在医院嘞，你怎么抢劫？"))
                    return
                }

                cn.chahuyun.economy.constant.UserLocation.FISHPOND -> {
                    group.sendMessage(MessageUtil.formatMessageChain(message, "钓鱼了，不要分心！"))
                    return
                }

                cn.chahuyun.economy.constant.UserLocation.PRISON -> {
                    group.sendMessage(MessageUtil.formatMessageChain(message, "还在局子里面咧，不要幻想!"))
                    return
                }

                else -> {
                    group.sendMessage(MessageUtil.formatMessageChain(message, "你还没有准备好抢劫!"))
                    return
                }
            }
        }

        val member: Member = ShareUtils.getAtMember(event) ?: run {
            group.sendMessage(MessageUtil.formatMessageChain(message, "你抢劫的人不在这里!"))
            return
        }

        val atUser: UserInfo = UserCoreManager.getUserInfo(member)

        if (UserStatusAction.checkUserNotInHome(atUser)) {
            val userStatus: UserStatus = UserStatusAction.getUserStatus(atUser)
            when (userStatus.place) {
                cn.chahuyun.economy.constant.UserLocation.HOSPITAL -> {
                    group.sendMessage(MessageUtil.formatMessageChain(message, "医院禁止抢劫/打人！"))
                    return
                }

                cn.chahuyun.economy.constant.UserLocation.PRISON -> {
                    group.sendMessage(MessageUtil.formatMessageChain(message, "他还在局子里面，抢不到了！"))
                    return
                }

                else -> {}
            }
        }

        val thisRobInfo = getRobInfo(thisUser)
        val atRobInfo = getRobInfo(atUser)

        var nowTime = atRobInfo.nowTime
        if (nowTime == null) {
            nowTime = Date()
            atRobInfo.nowTime = nowTime
        }

        if (DateUtil.isSameDay(Date(), nowTime)) {
            val beRobNumber = atRobInfo.beRobNumber ?: 0
            if (beRobNumber >= 20) {
                group.sendMessage(MessageUtil.formatMessageChain(message, "他今天已经被抢了20次了，上帝都觉得他可怜！"))
                return
            }
        } else {
            atRobInfo.nowTime = Date()
            atRobInfo.beRobNumber = 0
        }

        val userFactor: UserFactor = FactorManager.getUserFactor(thisUser)
        val atUserFactor: UserFactor = FactorManager.getUserFactor(atUser)

        if (BackpackManager.checkPropInUser(thisUser, FunctionProps.ELECTRIC_BATON)) {
            val prop: UserBackpack = thisUser.getProp(FunctionProps.ELECTRIC_BATON)

            if (PropsManager.usePropJava(prop, UseEvent(user, group, thisUser)).success) {
                userFactor.force = userFactor.force + 0.3
                group.sendMessage(MessageUtil.formatMessageChain(message, "你携带了便携电棒，攻击性变强了!"))
            } else {
                group.sendMessage(MessageUtil.formatMessageChain(message, "你的电棒好像有点问题.."))
            }
        }

        if (BackpackManager.checkPropInUser(atUser, FunctionProps.ELECTRIC_BATON)) {
            val prop: UserBackpack = atUser.getProp(FunctionProps.ELECTRIC_BATON)

            if (PropsManager.usePropJava(prop, UseEvent(user, group, atUser)).success) {
                atUserFactor.dodge = atUserFactor.dodge + 0.2
                atUserFactor.irritable = atUserFactor.irritable + 0.4
                group.sendMessage(MessageUtil.formatMessageChain(message, "对方掏出了便携电棒，局势变得紧张了起来!"))
            }
        }

        val robFactor = ShareUtils.percentageToInt(FactorManager.getGlobalFactor().robFactor)
        val robRandom = RandomUtil.randomInt(0, 101)

        var force = ShareUtils.percentageToInt(userFactor.force)
        var atDoge = ShareUtils.percentageToInt(atUserFactor.dodge)

        if (TitleManager.checkTitleIsOnEnable(thisUser, TitleCode.ROB)) {
            force += 10
        }

        if (TitleManager.checkTitleIsOnEnable(atUser, TitleCode.ROB)) {
            atDoge += 10
            atUserFactor.irritable = atUserFactor.irritable + 0.2
        }

        if ((robRandom - force) <= (robFactor - atDoge)) {
            val atMoney = EconomyUtil.getMoneyByUser(member)
            val thatLowMoney = 50.0

            if (atMoney < thatLowMoney) {
                val hit = RandomUtil.randomInt(0, 101)
                val irritable = ShareUtils.percentageToInt(userFactor.irritable)

                if (hit <= irritable) {
                    group.sendMessage(
                        MessageUtil.formatMessageChain(
                            message,
                            "你把他全身搜了个遍，连%.0f块钱都拿不出来，你气不过，打了他一顿，把他打进医院了。",
                            thatLowMoney
                        )
                    )

                    UserStatusAction.moveHospital(atUser, RandomUtil.randomInt(50, 501))
                } else {
                    group.sendMessage(MessageUtil.formatMessageChain(message, "他就穷光蛋一个，出门身上一块钱没有，真悲哀。"))
                }
                return
            }

            val resistance = ShareUtils.percentageToInt(atUserFactor.resistance)

            val moneyRandom = if (ShareUtils.randomCompare(resistance)) {
                val amount = ShareUtils.rounding(RandomUtil.randomDouble(0.0, atMoney / 2))
                group.sendMessage(
                    MessageUtil.formatMessageChain(
                        message,
                        "对方拼命反抗，但是你还是抢到了对方%.1f的金币跑了...",
                        amount
                    )
                )
                amount
            } else {
                group.sendMessage(
                    MessageUtil.formatMessageChain(
                        message,
                        "这次抢劫很顺利，对方看起来弱小可怜又无助，你抢了他全部的%.1f金币",
                        atMoney
                    )
                )
                atMoney
            }

            EconomyUtil.plusMoneyToUser(user, moneyRandom)
            EconomyUtil.minusMoneyToUser(member, moneyRandom)

            val success = thisRobInfo.robSuccess ?: 0
            val robSuccess = success + 1
            if (robSuccess == 50 && !TitleManager.checkTitleIsExist(thisUser, TitleCode.ROB)) {
                TitleManager.addTitleInfo(thisUser, TitleCode.ROB)
                group.sendMessage(MessageUtil.formatMessageChain(user.id, "你成功抢劫50次，获得 街区传说 称号！"))
            }
            thisRobInfo.robSuccess = robSuccess
            atRobInfo.beRobNumber = success + 1
            HibernateFactory.merge(thisRobInfo)
            HibernateFactory.merge(atRobInfo)
            return
        }

        val dodge = ShareUtils.percentageToInt(userFactor.dodge)
        if (robRandom - dodge > 75) {
            val quantity = 300
            EconomyUtil.minusMoneyToUser(user, quantity.toDouble())

            val recovery = 20
            UserStatusAction.movePrison(thisUser, recovery)

            group.sendMessage(
                MessageUtil.formatMessageChain(
                    message,
                    "你在抢劫的过程中，被警察发现了。%n被罚款%d金币，且拘留%d分钟",
                    quantity,
                    recovery
                )
            )
        } else {
            group.sendMessage(MessageUtil.formatMessageChain(message, "你正在实施抢劫，突然听见警笛，你头也不回的就跑了!"))
        }
    }

    @MessageAuthorize(
        text = ["打人 ?@?\\d{6,11} ?"],
        messageMatching = MessageMatchingEnum.REGULAR,
        messageConversion = MessageConversionEnum.CONTENT,
        groupPermissions = [EconPerm.ROB_PERM]
    )
    suspend fun hit(event: GroupMessageEvent) {
        event.group.sendMessage(MessageUtil.formatMessageChain(event.message, "练练再来吧！"))
    }

    @MessageAuthorize(text = ["开启 抢劫"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun startRob(event: GroupMessageEvent) {
        val group: Group = event.group
        val util = PermUtil.INSTANCE
        val user = UserUtil.INSTANCE.group(group.id)

        if (util.checkUserHasPerm(user, EconPerm.ROB_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的抢劫已经开启了!"))
            return
        }

        if (util.addUserToPermGroupByName(user, EconPerm.GROUP.ROB_PERM_GROUP)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的抢劫开启成功!"))
        } else {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的抢劫开启失败!"))
        }
    }

    @MessageAuthorize(text = ["关闭 抢劫"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun endRob(event: GroupMessageEvent) {
        val group: Group = event.group
        val util = PermUtil.INSTANCE
        val user = UserUtil.INSTANCE.group(group.id)

        if (!util.checkUserHasPerm(user, EconPerm.ROB_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的抢劫已经关闭!"))
            return
        }

        val permGroup: PermGroup = util.takePermGroupByName(EconPerm.GROUP.ROB_PERM_GROUP)
        permGroup.users.remove(user)
        permGroup.save()

        group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的抢劫关闭成功!"))
    }

    companion object {
        private val cooling: MutableMap<User, Date> = HashMap()

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
}
