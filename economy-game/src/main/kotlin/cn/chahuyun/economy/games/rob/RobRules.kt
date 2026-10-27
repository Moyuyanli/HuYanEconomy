package cn.chahuyun.economy.games.rob

import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.actionchain.annotation.Before
import cn.chahuyun.economy.actionchain.annotation.FromProvider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.actionchain.runtime.ActionResult
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.constant.UserLocation
import cn.chahuyun.economy.feature.privatebank.PrivateBankManager
import cn.chahuyun.economy.model.rob.RobInfoDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.service.EconomyUserService
import cn.chahuyun.economy.service.EconomyUserStatusService
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.message.data.At
import java.util.*

/**
 * 抢劫功能的规则链：`@Before` 前置规则集合。
 *
 * ## game.rob.rob 的执行顺序
 * ```
 * @Before(order=10) validateNoUnpaidLoan   欠贷 → 拦截（与转账同一口径）
 * @Before(order=20) consumeCooldown        冷却未过 → 拦截；通过则立刻打上冷却
 * @Before(order=30) resolveRobAttempt      位置/目标/当日被抢次数 → 解析出 RobAttempt
 * ------------------ RobUsecase.rob ------------------  道具加成 + 结算 + 记账
 * ```
 *
 * **order=20 有意承担副作用**：原实现在「冷却校验通过后、位置校验之前」就打上冷却，
 * 因此人在医院/局子时按一次抢劫同样消耗 10 分钟。把打冷却放在规则里而不是动作里，
 * 才能保持这个「失败也计时」的既有手感——顺序一旦换到动作内，非法状态就不再扣冷却。
 *
 * 开启/关闭两条管理指令的「是否已开启」判断也在这里：它是纯读校验，
 * 动作只负责真正改组并回复成功。
 */
object RobRules {

    /** 单个用户两次抢劫之间的冷却时间，单位：分钟。 */
    private const val COOLDOWN_MINUTES = 10L

    /** 单人单日最多被抢次数。 */
    private const val MAX_ROBBED_PER_DAY = 20

    /**
     * 前置规则（order=10）：有未还贷款时禁止抢劫。
     *
     * 与转账共用同一条追缴口径：允许抢劫会让债务人绕开自动扣缴。
     *
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "game.rob.rob", order = 10)
    suspend fun validateNoUnpaidLoan(
        @FromProvider(RobProvider.ACTOR) actor: RobActor,
    ): ActionResult =
        if (PrivateBankManager.hasUnrepaidLoans(actor.user.id)) {
            ActionResult.EndWithMessage("你还有未还贷款，暂时不能抢劫")
        } else {
            ActionResult.Continue
        }

    /**
     * 前置规则（order=20）：冷却校验，通过即打上冷却。
     *
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "game.rob.rob", order = 20)
    suspend fun consumeCooldown(
        @FromProvider(RobProvider.ACTOR) actor: RobActor,
    ): ActionResult {
        val remaining = RobService.getCoolingRemainingMinutes(actor.user.id, COOLDOWN_MINUTES)
        if (remaining > 0) {
            return ActionResult.EndWithMessage("再等等吧，你还得歇${remaining}分钟才能行动!")
        }
        RobService.markCooling(actor.user.id)
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=30）：把一次抢劫所需的全部实体与限制一次性解析出来。
     *
     * 依次校验：发起人位置 → 目标是否在群内 → 目标位置 → 目标当日被抢次数。
     * 全部通过后把 [RobAttempt] 放进上下文，动作里只做结算，不再查库。
     *
     * @param context 本次调用上下文，用于放入解析结果。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "game.rob.rob", order = 30)
    suspend fun resolveRobAttempt(
        context: ActionContext,
        @FromProvider(RobProvider.ACTOR) actor: RobActor,
    ): ActionResult {
        val thisUser: UserInfoDto = EconomyUserService.getOrCreate(actor.user)
        robberBlockedReason(thisUser)?.let { return ActionResult.EndWithMessage(it) }

        val member = actor.message.filterIsInstance<At>().firstOrNull()
            ?.let { actor.subject[it.target] }
            ?: return ActionResult.EndWithMessage("你抢劫的人不在这里!")
        val atUser: UserInfoDto = EconomyUserService.getOrCreate(member)
        targetBlockedReason(atUser)?.let { return ActionResult.EndWithMessage(it) }

        val atRobInfo = normalizedRobInfo(atUser)
        if (atRobInfo.beRobNumber >= MAX_ROBBED_PER_DAY) {
            return ActionResult.EndWithMessage("他今天已经被抢了$MAX_ROBBED_PER_DAY 次了，上帝都觉得他可怜！")
        }

        context.put(RobAttempt::class, RobAttempt(thisUser, atUser, atRobInfo))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：本群还没开抢劫才允许开启。
     *
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "game.rob.open", order = 10)
    suspend fun validateNotEnabled(
        @FromProvider(RobProvider.ACTOR) actor: RobActor,
    ): ActionResult {
        val group = UserUtil.group(actor.subject.id)
        return if (PermUtil.checkUserHasPerm(group, EconPerm.ROB_PERM)) {
            ActionResult.EndWithMessage("本群的抢劫已经开启了!")
        } else {
            ActionResult.Continue
        }
    }

    /**
     * 前置规则（order=10）：只有已开启的群才允许关闭。
     *
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "game.rob.close", order = 10)
    suspend fun validateEnabled(
        @FromProvider(RobProvider.ACTOR) actor: RobActor,
    ): ActionResult {
        val group = UserUtil.group(actor.subject.id)
        return if (PermUtil.checkUserHasPerm(group, EconPerm.ROB_PERM)) {
            ActionResult.Continue
        } else {
            ActionResult.EndWithMessage("本群的抢劫已经关闭!")
        }
    }

    /**
     * 发起人位置校验；返回 null 表示可以动手。
     *
     * 注意「不在家」一律拦截：原实现对未知地点也给同一句提示，不是放行分支。
     */
    private fun robberBlockedReason(thisUser: UserInfoDto): String? {
        if (!EconomyUserStatusService.isNotHome(thisUser)) return null
        return when (EconomyUserStatusService.getStatus(thisUser.qq).place) {
            UserLocation.HOSPITAL.name -> "你还在医院嘞，你怎么抢劫？"
            UserLocation.FISHPOND.name -> "钓鱼了，不要分心！"
            UserLocation.PRISON.name -> "还在局子里面咧，不要幻想!"
            else -> "你还没有准备好抢劫!"
        }
    }

    /**
     * 目标位置校验；返回 null 表示可以抢。
     *
     * 与发起人不同，目标在鱼塘是允许被抢的——原实现只拦医院与局子，这里保持一致。
     */
    private fun targetBlockedReason(atUser: UserInfoDto): String? {
        if (!EconomyUserStatusService.isNotHome(atUser)) return null
        return when (EconomyUserStatusService.getStatus(atUser.qq).place) {
            UserLocation.HOSPITAL.name -> "医院禁止抢劫/打人！"
            UserLocation.PRISON.name -> "他还在局子里面，抢不到了！"
            else -> null
        }
    }

    /**
     * 取出目标的被抢记录，并按「是否跨天」决定要不要重置当日计数。
     *
     * 跨天时把 `nowTime` 推进到当前时间、`beRobNumber` 归零；同一天则原样返回。
     */
    private fun normalizedRobInfo(atUser: UserInfoDto): RobInfoDto {
        val info = RobService.getRobInfo(atUser)
        val nowTime = info.nowTime.takeIf { it > 0 } ?: return info.copy(nowTime = Date().time)
        return if (DateUtil.isSameDay(Date(), Date(nowTime))) {
            info
        } else {
            info.copy(nowTime = Date().time, beRobNumber = 0)
        }
    }
}

/**
 * 一次抢劫指令解析出的全部参与者与状态。
 *
 * 由 [RobRules.resolveRobAttempt] 产出，[RobUsecase.rob] 直接消费，
 * 动作里不再重复查库、也不重复判定位置。
 *
 * @property robber 发起人资料。
 * @property target 被抢者资料。
 * @property targetRobInfo 被抢者已归一化的记录（当日计数已按跨天规则重置）。
 */
data class RobAttempt(
    val robber: UserInfoDto,
    val target: UserInfoDto,
    val targetRobInfo: RobInfoDto,
)
