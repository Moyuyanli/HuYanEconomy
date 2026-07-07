package cn.chahuyun.economy.usecase

import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.constant.TitleCode
import cn.chahuyun.economy.model.props.FunctionProps
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.model.user.getProp
import cn.chahuyun.economy.service.*
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MoneyFormatUtil
import cn.chahuyun.economy.utils.ShareUtils
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain
import java.util.*

object RobUsecase {

    private const val COOLDOWN_MINUTES = 10L

    suspend fun rob(event: GroupMessageEvent) {
        Log.info("抢劫指令")

        val group: Group = event.group
        val message: MessageChain = event.message

        val user: Member = event.sender
        val thisUser = EconomyUserService.getOrCreate(user)

        val remaining = RobService.getCoolingRemainingMinutes(user.id, COOLDOWN_MINUTES)
        if (remaining > 0) {
            GameUsecaseReplySupport.reply(group, message, "再等等吧，你还得歇${remaining}分钟才能行动!")
            return
        }
        RobService.markCooling(user.id)

        if (EconomyUserStatusService.isNotHome(thisUser)) {
            val userStatus = EconomyUserStatusService.getStatus(user.id)
            when (userStatus.place) {
                cn.chahuyun.economy.constant.UserLocation.HOSPITAL.name -> {
                    GameUsecaseReplySupport.reply(group, message, "你还在医院嘞，你怎么抢劫？")
                    return
                }

                cn.chahuyun.economy.constant.UserLocation.FISHPOND.name -> {
                    GameUsecaseReplySupport.reply(group, message, "钓鱼了，不要分心！")
                    return
                }

                cn.chahuyun.economy.constant.UserLocation.PRISON.name -> {
                    GameUsecaseReplySupport.reply(group, message, "还在局子里面咧，不要幻想!")
                    return
                }

                else -> {
                    GameUsecaseReplySupport.reply(group, message, "你还没有准备好抢劫!")
                    return
                }
            }
        }

        val member: Member = ShareUtils.getAtMember(event) ?: run {
            GameUsecaseReplySupport.reply(group, message, "你抢劫的人不在这里!")
            return
        }

        val atUser = EconomyUserService.getOrCreate(member)

        if (EconomyUserStatusService.isNotHome(atUser)) {
            val userStatus = EconomyUserStatusService.getStatus(atUser)
            when (userStatus.place) {
                cn.chahuyun.economy.constant.UserLocation.HOSPITAL.name -> {
                    GameUsecaseReplySupport.reply(group, message, "医院禁止抢劫/打人！")
                    return
                }

                cn.chahuyun.economy.constant.UserLocation.PRISON.name -> {
                    GameUsecaseReplySupport.reply(group, message, "他还在局子里面，抢不到了！")
                    return
                }

                else -> {}
            }
        }

        var thisRobInfo = RobService.getRobInfo(thisUser)
        var atRobInfo = RobService.getRobInfo(atUser)

        var nowTime = atRobInfo.nowTime.takeIf { it > 0 }
        if (nowTime == null) {
            nowTime = Date().time
            atRobInfo = atRobInfo.copy(nowTime = nowTime)
        }

        if (cn.hutool.core.date.DateUtil.isSameDay(Date(), Date(nowTime))) {
            val beRobNumber = atRobInfo.beRobNumber
            if (beRobNumber >= 20) {
                GameUsecaseReplySupport.reply(group, message, "他今天已经被抢了20次了，上帝都觉得他可怜！")
                return
            }
        } else {
            atRobInfo = atRobInfo.copy(nowTime = Date().time, beRobNumber = 0)
        }

        var userFactor = EconomyFactorService.userFactor(thisUser)
        var atUserFactor = EconomyFactorService.userFactor(atUser)

        if (EconomyInventoryService.hasProp(thisUser, FunctionProps.ELECTRIC_BATON)) {
            val prop = thisUser.getProp(FunctionProps.ELECTRIC_BATON)

            if (EconomyInventoryService.usePropSync(prop, UseEvent(user, group, thisUser)).success) {
                userFactor = userFactor.copy(force = userFactor.force + 0.3)
                GameUsecaseReplySupport.reply(group, message, "你携带了便携电棒，攻击性变强了!")
            } else {
                GameUsecaseReplySupport.reply(group, message, "你的电棒好像有点问题..")
            }
        }

        if (EconomyInventoryService.hasProp(atUser, FunctionProps.ELECTRIC_BATON)) {
            val prop = atUser.getProp(FunctionProps.ELECTRIC_BATON)

            if (EconomyInventoryService.usePropSync(prop, UseEvent(user, group, atUser)).success) {
                atUserFactor = atUserFactor.copy(
                    dodge = atUserFactor.dodge + 0.2,
                    irritable = atUserFactor.irritable + 0.4
                )
                GameUsecaseReplySupport.reply(group, message, "对方掏出了便携电棒，局势变得紧张了起来!")
            }
        }

        val robFactor = ShareUtils.percentageToInt(EconomyFactorService.globalFactor().robFactor)
        val robRandom = RandomUtil.randomInt(0, 101)

        var force = ShareUtils.percentageToInt(userFactor.force)
        var atDoge = ShareUtils.percentageToInt(atUserFactor.dodge)

        if (EconomyTitleService.isEnabled(thisUser, TitleCode.ROB)) {
            force += 10
        }

        if (EconomyTitleService.isEnabled(atUser, TitleCode.ROB)) {
            atDoge += 10
            atUserFactor = atUserFactor.copy(irritable = atUserFactor.irritable + 0.2)
        }

        if ((robRandom - force) <= (robFactor - atDoge)) {
            val atMoney = EconomyAccountService.walletBalance(member)
            val thatLowMoney = 50.0

            if (atMoney < thatLowMoney) {
                val hit = RandomUtil.randomInt(0, 101)
                val irritable = ShareUtils.percentageToInt(userFactor.irritable)

                if (hit <= irritable) {
                    GameUsecaseReplySupport.reply(
                        group,
                        message,
                        "你把他全身搜了个遍，连${MoneyFormatUtil.format(thatLowMoney)}块钱都拿不出来，你气不过，打了他一顿，把他打进医院了。"
                    )

                    EconomyUserStatusService.moveHospital(atUser, RandomUtil.randomInt(50, 501))
                } else {
                    GameUsecaseReplySupport.reply(group, message, "他就穷光蛋一个，出门身上一块钱没有，真悲哀。")
                }
                return
            }

            val resistance = ShareUtils.percentageToInt(atUserFactor.resistance)

            val moneyRandom = if (ShareUtils.randomCompare(resistance)) {
                val amount = ShareUtils.rounding(RandomUtil.randomDouble(0.0, atMoney / 2))
                GameUsecaseReplySupport.reply(
                    group,
                    message,
                    "对方拼命反抗，但是你还是抢到了对方${MoneyFormatUtil.format(amount)}的金币跑了..."
                )
                amount
            } else {
                GameUsecaseReplySupport.reply(
                    group,
                    message,
                    "这次抢劫很顺利，对方看起来弱小可怜又无助，你抢了他全部的${MoneyFormatUtil.format(atMoney)}金币"
                )
                atMoney
            }

            EconomyAccountService.addWallet(user, moneyRandom)
            EconomyAccountService.subtractWallet(member, moneyRandom)

            val success = thisRobInfo.robSuccess
            val robSuccess = success + 1
            if (robSuccess == 50 && !EconomyTitleService.exists(thisUser, TitleCode.ROB)) {
                EconomyTitleService.grant(thisUser, TitleCode.ROB)
                GameUsecaseReplySupport.reply(group, user.id, "你成功抢劫50次，获得 街区传说 称号！")
            }
            thisRobInfo = thisRobInfo.copy(robSuccess = robSuccess)
            atRobInfo = atRobInfo.copy(beRobNumber = success + 1)
            RobService.saveRobInfo(thisRobInfo)
            RobService.saveRobInfo(atRobInfo)
            return
        }

        val dodge = ShareUtils.percentageToInt(userFactor.dodge)
        if (robRandom - dodge > 75) {
            val quantity = 300
            EconomyAccountService.subtractWallet(user, quantity.toDouble())

            val recovery = 20
            EconomyUserStatusService.movePrison(thisUser, recovery)

            GameUsecaseReplySupport.reply(
                group,
                message,
                "你在抢劫的过程中，被警察发现了。\n被罚款${MoneyFormatUtil.format(quantity.toDouble())}金币，且拘留${recovery}分钟"
            )
        } else {
            GameUsecaseReplySupport.reply(group, message, "你正在实施抢劫，突然听见警笛，你头也不回的就跑了!")
        }
    }

    suspend fun hit(event: GroupMessageEvent) {
        GameUsecaseReplySupport.reply(event, "练练再来吧！")
    }

    suspend fun startRob(event: GroupMessageEvent) {
        val group: Group = event.group
        val util = PermUtil
        val user = UserUtil.group(group.id)

        if (util.checkUserHasPerm(user, EconPerm.ROB_PERM)) {
            GameUsecaseReplySupport.reply(event, "本群的抢劫已经开启了!")
            return
        }

        if (util.addUserToPermGroupByName(user, EconPerm.GROUP.ROB_PERM_GROUP)) {
            GameUsecaseReplySupport.reply(event, "本群的抢劫开启成功!")
        } else {
            GameUsecaseReplySupport.reply(event, "本群的抢劫开启失败!")
        }
    }

    suspend fun endRob(event: GroupMessageEvent) {
        val group: Group = event.group
        val util = PermUtil
        val user = UserUtil.group(group.id)

        if (!util.checkUserHasPerm(user, EconPerm.ROB_PERM)) {
            GameUsecaseReplySupport.reply(event, "本群的抢劫已经关闭!")
            return
        }

        val permGroup: PermGroup = util.takePermGroupByName(EconPerm.GROUP.ROB_PERM_GROUP)
        permGroup.users.remove(user)
        permGroup.save()

        GameUsecaseReplySupport.reply(event, "本群的抢劫关闭成功!")
    }
}
