package cn.chahuyun.economy.feature.status

import cn.chahuyun.economy.actionchain.annotation.After
import cn.chahuyun.economy.actionchain.annotation.Before
import cn.chahuyun.economy.actionchain.annotation.FromProvider
import cn.chahuyun.economy.actionchain.runtime.ActionResult
import cn.chahuyun.economy.utils.Log

/**
 * 用户状态功能的规则链：`@Before` / `@After` 规则集合。
 *
 * ## core.status.home
 * ```
 * @Before(order=10) validateGoHome    已在家？在医院/监狱？→ 拦截
 * ------------------ UserStatusUsecase.goHome ------------------  moveHome + 回复
 * ```
 *
 * ## core.status.discharge
 * ```
 * @Before(order=10) validateDischarge 不在医院？→ 拦截
 * ------------------ UserStatusUsecase.discharge ------------------  算费 + 扣款 + moveHome
 * @After(order=10)  logDischarged     出院留痕
 * ```
 *
 * 位置判定放进规则后，将来要做「住院期间禁止签到」「监狱期间不能偷菜」，
 * 只需在对应动作上加规则，不必再改状态功能本体。
 */
object UserStatusRules {

    /**
     * 前置规则（order=10）：回家资格校验。
     *
     * 与原实现顺序一致：已在家 → 提示；在医院或监狱 → 不许回家。
     *
     * @param actor 本次状态指令的参与者快照。
     */
    @Before(actionId = "core.status.home", order = 10)
    suspend fun validateGoHome(
        @FromProvider("status-actor") actor: UserStatusActor,
    ): ActionResult {
        if (UserStatusManager.checkUserInHome(actor.userInfo)) {
            return ActionResult.EndWithMessage("你已经在家里了！")
        }
        if (UserStatusManager.checkUserInHospital(actor.userInfo) ||
            UserStatusManager.checkUserInPrison(actor.userInfo)
        ) {
            return ActionResult.EndWithMessage("你现在还不能回家！")
        }
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：出院资格校验。
     *
     * 不在医院时直接结束，动作不会执行，也不会误扣医药费。
     *
     * @param actor 本次状态指令的参与者快照。
     */
    @Before(actionId = "core.status.discharge", order = 10)
    suspend fun validateDischarge(
        @FromProvider("status-actor") actor: UserStatusActor,
    ): ActionResult {
        return if (UserStatusManager.checkUserInHospital(actor.userInfo)) {
            ActionResult.Continue
        } else {
            ActionResult.EndWithMessage("你不在医院，你出什么院？")
        }
    }

    /**
     * 后置规则（order=10）：出院留痕。
     *
     * 只记录「谁在哪个群出院」，不判断扣款结果——失败时同样需要可追溯。
     *
     * @param actor 本次状态指令的参与者快照。
     */
    @After(actionId = "core.status.discharge", order = 10)
    suspend fun logDischarged(
        @FromProvider("status-actor") actor: UserStatusActor,
    ) {
        Log.info("出院:群(${actor.group.id}) 用户(${actor.member.id})")
    }
}
