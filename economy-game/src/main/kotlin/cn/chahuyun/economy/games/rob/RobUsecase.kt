package cn.chahuyun.economy.games.rob

import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.actionchain.annotation.Action
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.constant.TitleCode
import cn.chahuyun.economy.feature.privatebank.PrivateBankService
import cn.chahuyun.economy.games.GamesReplySupport
import cn.chahuyun.economy.model.props.FunctionProps
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.model.user.UserFactorDto
import cn.chahuyun.economy.model.user.getProp
import cn.chahuyun.economy.service.*
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MoneyFormatUtil
import cn.chahuyun.economy.utils.ShareUtils
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 抢劫玩法的主用例。
 *
 * ## 动作清单
 * | actionId | 指令 | 规则链 |
 * |---|---|---|
 * | `game.rob.rob` | 抢劫 @用户 | `@Before(10)` 欠贷 → `@Before(20)` 冷却（通过即计时）→ `@Before(30)` 位置/目标/当日次数 |
 * | `game.rob.hit` | 打人 @用户 | 无规则；占位动作，直接提示「练练再来吧」 |
 * | `game.rob.open` | 开启 抢劫 | `@Before(10)` 是否已开启 |
 * | `game.rob.close` | 关闭 抢劫 | `@Before(10)` 是否已开启 |
 *
 * 除「打人」外，所有拦截判定都在 [RobRules]；本文件只做加成计算、结算与回复。
 */
object RobUsecase {

    /** 被抢者余额低于该值时改为「人身攻击」分支。 */
    private const val POOR_THRESHOLD = 50.0

    /** 抢劫失败被警察抓到时罚款金额。 */
    private const val FINE = 300

    /** 抢劫失败被警察抓到时拘留分钟数。 */
    private const val PRISON_MINUTES = 20

    /**
     * 实施抢劫（`game.rob.rob`）。
     *
     * 参与者与目标状态由 [RobRules.resolveRobAttempt] 放进 [RobAttempt]，
     * 这里只处理电棒道具加成、成功率掷点与三种结局（抢到 / 对方太穷 / 被警察抓）。
     *
     * @param event 群消息事件，用于回复与道具使用场景。
     * @param context 本次调用上下文，读取 [RobAttempt]。
     */
    @Action("game.rob.rob")
    suspend fun rob(event: GroupMessageEvent, context: ActionContext) {
        Log.info("抢劫指令")

        val actor = context.provide(RobActor::class, RobProvider.ACTOR)
        val attempt = context.get(RobAttempt::class) ?: return
        val robber = attempt.robber
        val target = attempt.target
        val group = actor.subject
        val message = actor.message
        val user = actor.user
        val member = group[target.qq] ?: return

        var robberFactor = EconomyFactorService.userFactor(robber)
        var targetFactor = EconomyFactorService.userFactor(target)

        if (EconomyInventoryService.hasProp(robber, FunctionProps.ELECTRIC_BATON)) {
            val prop = robber.getProp(FunctionProps.ELECTRIC_BATON)
            if (EconomyInventoryService.useProp(prop, UseEvent(user, group, robber)).success) {
                robberFactor = robberFactor.copy(force = robberFactor.force + 0.3)
                GamesReplySupport.reply(group, message, "你携带了便携电棒，攻击性变强了!")
            } else {
                GamesReplySupport.reply(group, message, "你的电棒好像有点问题..")
            }
        }

        if (EconomyInventoryService.hasProp(target, FunctionProps.ELECTRIC_BATON)) {
            val prop = target.getProp(FunctionProps.ELECTRIC_BATON)
            if (EconomyInventoryService.useProp(prop, UseEvent(user, group, target)).success) {
                targetFactor = targetFactor.copy(
                    dodge = targetFactor.dodge + 0.2,
                    irritable = targetFactor.irritable + 0.4,
                )
                GamesReplySupport.reply(group, message, "对方掏出了便携电棒，局势变得紧张了起来!")
            }
        }

        val robFactor = ShareUtils.percentageToInt(EconomyFactorService.globalFactor().robFactor)
        val robRandom = RandomUtil.randomInt(0, 101)

        var force = ShareUtils.percentageToInt(robberFactor.force)
        var targetDodge = ShareUtils.percentageToInt(targetFactor.dodge)

        if (EconomyTitleService.isEnabled(robber, TitleCode.ROB)) {
            force += 10
        }
        if (EconomyTitleService.isEnabled(target, TitleCode.ROB)) {
            targetDodge += 10
            targetFactor = targetFactor.copy(irritable = targetFactor.irritable + 0.2)
        }

        if ((robRandom - force) <= (robFactor - targetDodge)) {
            robSucceeded(event, attempt, robberFactor, targetFactor, member)
            return
        }

        val dodge = ShareUtils.percentageToInt(robberFactor.dodge)
        if (robRandom - dodge > 75) {
            EconomyAccountService.subtractWallet(user, FINE.toDouble())
            EconomyUserStatusService.movePrison(robber, PRISON_MINUTES)
            GamesReplySupport.reply(
                group,
                message,
                "你在抢劫的过程中，被警察发现了。\n被罚款${MoneyFormatUtil.format(FINE.toDouble())}金币，且拘留${PRISON_MINUTES}分钟",
            )
        } else {
            GamesReplySupport.reply(group, message, "你正在实施抢劫，突然听见警笛，你头也不回的就跑了!")
        }
    }

    /**
     * 抢劫成功分支：抢钱、优先偿还逾期贷款、累计称号进度。
     *
     * @param event 群消息事件。
     * @param attempt 本次抢劫的参与者快照。
     * @param robberFactor 发起人因子（含电棒/称号加成）。
     * @param targetFactor 目标因子（含电棒/称号加成）。
     * @param member 目标群成员对象（扣款用）。
     */
    private suspend fun robSucceeded(
        event: GroupMessageEvent,
        attempt: RobAttempt,
        robberFactor: UserFactorDto,
        targetFactor: UserFactorDto,
        member: Member,
    ) {
        val robber = attempt.robber
        val target = attempt.target
        val group = event.group
        val message = event.message
        val user = event.sender

        val targetBalance = EconomyAccountService.walletBalance(member)
        if (targetBalance < POOR_THRESHOLD) {
            val hit = RandomUtil.randomInt(0, 101)
            val irritable = ShareUtils.percentageToInt(robberFactor.irritable)
            if (hit <= irritable) {
                GamesReplySupport.reply(
                    group,
                    message,
                    "你把他全身搜了个遍，连${MoneyFormatUtil.format(POOR_THRESHOLD)}块钱都拿不出来，你气不过，打了他一顿，把他打进医院了。",
                )
                EconomyUserStatusService.moveHospital(target, RandomUtil.randomInt(50, 501))
            } else {
                GamesReplySupport.reply(group, message, "他就穷光蛋一个，出门身上一块钱没有，真悲哀。")
            }
            return
        }

        val robRate = RandomUtil.randomDouble(0.05, 0.350001)
        val money = ShareUtils.rounding(targetBalance * robRate)
        val resistance = ShareUtils.percentageToInt(targetFactor.resistance)

        if (ShareUtils.randomCompare(resistance)) {
            GamesReplySupport.reply(
                group,
                message,
                "对方拼命反抗，但是你还是抢到了对方${MoneyFormatUtil.format(money)}的金币跑了...",
            )
        } else {
            GamesReplySupport.reply(
                group,
                message,
                "这次抢劫很顺利，你抢到了对方${MoneyFormatUtil.format(money)}的金币",
            )
        }

        if (!EconomyAccountService.subtractWallet(member, money)) {
            GamesReplySupport.reply(group, message, "抢劫结算失败，对方的钱包扣款失败")
            return
        }

        val repayment = PrivateBankService.repayOverdueLoansFromIncome(user.id, money)
        if (repayment.remaining > 0.0001) {
            EconomyAccountService.addWallet(user, repayment.remaining)
        }
        if (repayment.repaid > 0.0001) {
            GamesReplySupport.reply(
                group,
                user.id,
                "抢劫所得已优先偿还逾期贷款 ${MoneyFormatUtil.format(repayment.repaid)}，入账 ${MoneyFormatUtil.format(repayment.remaining)}",
            )
        }

        var robberRobInfo = RobService.getRobInfo(robber)
        val robSuccess = robberRobInfo.robSuccess + 1
        if (robSuccess == 50 && !EconomyTitleService.exists(robber, TitleCode.ROB)) {
            EconomyTitleService.grant(robber, TitleCode.ROB)
            GamesReplySupport.reply(group, user.id, "你成功抢劫50次，获得 街区传说 称号！")
        }
        robberRobInfo = robberRobInfo.copy(robSuccess = robSuccess)

        // NOTE: 沿袭既有实现——目标当日被抢次数写的是「抢劫者的累计成功次数」，
        // 而不是 targetRobInfo.beRobNumber + 1。这会让成功次数超过 20 的玩家无法再抢任何人。
        // 迁移只做结构整理，此处刻意不改语义，待玩法调整时单独修。
        val targetRobInfo = attempt.targetRobInfo.copy(beRobNumber = robSuccess)

        RobService.saveRobInfo(robberRobInfo)
        RobService.saveRobInfo(targetRobInfo)
    }

    /**
     * 打人（`game.rob.hit`）：尚未实现，固定回复一句提示。
     *
     * @param event 群消息事件。
     */
    @Action("game.rob.hit")
    suspend fun hit(event: GroupMessageEvent) {
        Log.info("打人指令:用户(${event.sender.id})")
        GamesReplySupport.reply(event, "练练再来吧！")
    }

    /**
     * 开启本群抢劫（`game.rob.open`）。
     *
     * @param event 群消息事件。
     */
    @Action("game.rob.open")
    suspend fun startRob(event: GroupMessageEvent) {
        Log.info("开启抢劫:群(${event.group.id})")
        val group = UserUtil.group(event.group.id)
        val message = if (PermUtil.addUserToPermGroupByName(group, EconPerm.GROUP.ROB_PERM_GROUP)) {
            "本群的抢劫开启成功!"
        } else {
            "本群的抢劫开启失败!"
        }
        GamesReplySupport.reply(event, message)
    }

    /**
     * 关闭本群抢劫（`game.rob.close`）。
     *
     * @param event 群消息事件。
     */
    @Action("game.rob.close")
    suspend fun endRob(event: GroupMessageEvent) {
        Log.info("关闭抢劫:群(${event.group.id})")
        val group = UserUtil.group(event.group.id)
        val permGroup: PermGroup = PermUtil.takePermGroupByName(EconPerm.GROUP.ROB_PERM_GROUP)
        permGroup.users.remove(group)
        permGroup.save()
        GamesReplySupport.reply(event, "本群的抢劫关闭成功!")
    }
}
