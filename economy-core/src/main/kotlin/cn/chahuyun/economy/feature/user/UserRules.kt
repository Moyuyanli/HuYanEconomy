package cn.chahuyun.economy.feature.user

import cn.chahuyun.economy.actionchain.annotation.After
import cn.chahuyun.economy.actionchain.annotation.Before
import cn.chahuyun.economy.actionchain.annotation.FromProvider
import cn.chahuyun.economy.feature.title.TitleManager
import cn.chahuyun.economy.utils.Log

/**
 * 用户功能的规则链：`@Before` / `@After` 规则集合。
 *
 * ## core.user.info / core.user.infoText
 * ```
 * @Before(order=10) refreshMonopolyTitle  金币过 10 万则补发「大富翁」称号
 * ------------------ UserUsecase.getUserInfoImage / getUserInfoText ------------------
 * @After(order=10)  logViewed             查看留痕（仅 info）
 * ```
 *
 * 为什么称号检查必须在前置：个人信息卡渲染的是「当前启用称号」，
 * 若放在 `@After`，本次卡片仍是旧称号，玩家要发第二次指令才看得到。
 */
object UserRules {

    /**
     * 前置规则（order=10）：查看个人信息前补发「大富翁」称号。
     *
     * 金币超过 10 万才触发，已拥有则直接返回（`TitleManager` 内部判重）。
     *
     * @param actor 本次用户指令的参与者快照。
     */
    @Before(actionId = "core.user.info", order = 10)
    suspend fun refreshMonopolyTitle(
        @FromProvider("user-actor") actor: UserActor,
    ) {
        TitleManager.checkMonopoly(actor.userInfo, actor.subject)
    }

    /**
     * 前置规则（order=10）：文字版同样先修正称号，保证图/文两版显示一致。
     *
     * @param actor 本次用户指令的参与者快照。
     */
    @Before(actionId = "core.user.infoText", order = 10)
    suspend fun refreshMonopolyTitleForText(
        @FromProvider("user-actor") actor: UserActor,
    ) {
        TitleManager.checkMonopoly(actor.userInfo, actor.subject)
    }

    /**
     * 后置规则（order=10）：个人信息查看留痕。
     *
     * 个人信息卡会读取余额与银行存款，属于敏感查询，留痕便于审计。
     *
     * @param actor 本次用户指令的参与者快照。
     */
    @After(actionId = "core.user.info", order = 10)
    suspend fun logViewed(
        @FromProvider("user-actor") actor: UserActor,
    ) {
        Log.info("个人信息查看:用户(${actor.member.id})")
    }
}
