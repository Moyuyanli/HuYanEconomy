package cn.chahuyun.economy.games.raffle

import cn.chahuyun.economy.actionchain.annotation.Before
import cn.chahuyun.economy.actionchain.annotation.FromProvider
import cn.chahuyun.economy.actionchain.runtime.ActionResult
import cn.chahuyun.economy.service.EconomyAccountService
import cn.chahuyun.economy.service.EconomyRaffleService

/**
 * 抽奖功能的规则链：`@Before` 前置规则集合。
 *
 * ## 单抽 / 十连的执行顺序
 * ```
 * @Before(order=10) validateCooldown  冷却未过 → 拦截（剩余秒数回显）
 * @Before(order=20) validateBalance   余额不足本次价格 → 拦截
 * ------------------ RaffleUsecase.draw / drawTen ------------------  扣款 + 抽奖 + 发奖 + 记冷却
 * ```
 *
 * 冷却与余额是两套独立状态（单抽 60 秒、十连 10 分钟），因此两条动作各有一对规则，
 * 不共用——共用会让「十连冷却」误拦单抽。
 */
object RaffleRules {

    /**
     * 前置规则（order=10）：单抽冷却。
     *
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "game.raffle.draw", order = 10)
    suspend fun validateSingleCooldown(
        @FromProvider(RaffleProvider.ACTOR) actor: RaffleActor,
    ): ActionResult {
        if (RaffleService.checkSingleCooldown(actor.user.id)) return ActionResult.Continue
        val remaining = RaffleService.singleRemainingSeconds(actor.user.id)
        return ActionResult.EndWithMessage("单抽冷却中，请再等 $remaining 秒")
    }

    /**
     * 前置规则（order=20）：单抽余额。
     *
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "game.raffle.draw", order = 20)
    suspend fun validateSingleBalance(
        @FromProvider(RaffleProvider.ACTOR) actor: RaffleActor,
    ): ActionResult {
        val price = EconomyRaffleService.defaultPoolPrice()
        return if (EconomyAccountService.walletBalance(actor.user) < price) {
            ActionResult.EndWithMessage("你的余额不足 $price ,无法抽奖")
        } else {
            ActionResult.Continue
        }
    }

    /**
     * 前置规则（order=10）：十连冷却。
     *
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "game.raffle.ten", order = 10)
    suspend fun validateTenCooldown(
        @FromProvider(RaffleProvider.ACTOR) actor: RaffleActor,
    ): ActionResult {
        if (RaffleService.checkTenCooldown(actor.user.id)) return ActionResult.Continue
        val remaining = RaffleService.tenRemainingSeconds(actor.user.id)
        return ActionResult.EndWithMessage("十连冷却中，请再等 $remaining 秒")
    }

    /**
     * 前置规则（order=20）：十连余额（单抽价 × 10）。
     *
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "game.raffle.ten", order = 20)
    suspend fun validateTenBalance(
        @FromProvider(RaffleProvider.ACTOR) actor: RaffleActor,
    ): ActionResult {
        val price = EconomyRaffleService.defaultPoolPrice(10)
        return if (EconomyAccountService.walletBalance(actor.user) < price) {
            ActionResult.EndWithMessage("你的余额不足 $price ,无法抽奖")
        } else {
            ActionResult.Continue
        }
    }
}
