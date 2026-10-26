package cn.chahuyun.economy.games.lottery

import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.actionchain.annotation.Before
import cn.chahuyun.economy.actionchain.annotation.FromProvider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.actionchain.runtime.ActionResult
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.service.EconomyAccountService
import cn.chahuyun.economy.utils.MoneyFormatUtil

/**
 * 猜签功能的规则链：`@Before` 前置规则集合。
 *
 * ## game.lottery.bet 的执行顺序
 * ```
 * @Before(order=10) parseBet        解析号码与金额（金额格式错 → 拦截）
 * @Before(order=20) validateBalance 余额必须大于投注额
 * @Before(order=30) resolveBetType  按号码位数定签种并校验投注上限 → 放入 LotteryBet
 * ------------------ LotteryUsecase.bet ------------------  扣款 + 落库 + 排期
 * ```
 *
 * 校验顺序与原实现完全一致：**先看钱、再看签种**。所以「穷 + 号码写错」时用户看到的是
 * 「你都穷的叮当响了」，而不是「猜签类型错误」——这是有意的既有口径，不要调换。
 *
 * 开启/关闭的判断同样在规则里：动作只做权限组的真实增删与成功回复。
 */
object LotteryRules {

    /** 小签：三位号码，单注上限 1000。 */
    private const val SMALL_MAX_BET = 1000.0

    /** 中签：四位号码，单注上限 10000。 */
    private const val MEDIUM_MAX_BET = 10_000.0

    /** 大签：五位号码，单注上限 1000000。 */
    private const val LARGE_MAX_BET = 1_000_000.0

    /**
     * 前置规则（order=10）：解析投注号码与金额。
     *
     * 指令形如 `猜签 123 100`，号码段保留原始字符串，签种在下一步再判。
     *
     * @param context 本次调用上下文，用于放入解析结果。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "game.lottery.bet", order = 10)
    suspend fun parseBet(
        context: ActionContext,
        @FromProvider(LotteryProvider.ACTOR) actor: LotteryActor,
    ): ActionResult {
        val segments = actor.message.serializeToMiraiCode().split(" ")
        val number = segments.getOrNull(1)
            ?: return ActionResult.EndWithMessage("猜签类型错误!")
        val money = segments.getOrNull(2)?.let { MoneyFormatUtil.parse(it) }
            ?: return ActionResult.EndWithMessage("金额格式错误!")

        context.put(LotteryBetDraft::class, LotteryBetDraft(number = number, money = money))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=20）：余额必须严格大于投注额。
     *
     * 原实现用的是 `余额 - 投注额 <= 0` 即拒绝，因此「刚好等于」也不允许投注。
     *
     * @param context 本次调用上下文，读取上一步的解析结果。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "game.lottery.bet", order = 20)
    suspend fun validateBalance(
        context: ActionContext,
        @FromProvider(LotteryProvider.ACTOR) actor: LotteryActor,
    ): ActionResult {
        val draft = context.get(LotteryBetDraft::class) ?: return ActionResult.Continue
        val balance = EconomyAccountService.walletBalance(actor.user)
        return if (balance - draft.money <= 0) {
            ActionResult.EndWithMessage("你都穷的叮当响了，还来猜签？")
        } else {
            ActionResult.Continue
        }
    }

    /**
     * 前置规则（order=30）：按号码位数确定签种，并校验该签种的单注上限。
     *
     * 号码落库前会被改写成逗号分隔（`123` → `1,2,3`），与开签时的比较方式保持一致。
     *
     * @param context 本次调用上下文，读取解析结果并放入最终的 [LotteryBet]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "game.lottery.bet", order = 30)
    suspend fun resolveBetType(
        context: ActionContext,
    ): ActionResult {
        val draft = context.get(LotteryBetDraft::class) ?: return ActionResult.Continue

        val (type, typeName, maxBet) = when (draft.number.length) {
            3 -> Triple(1, "小签", SMALL_MAX_BET)
            4 -> Triple(2, "中签", MEDIUM_MAX_BET)
            5 -> Triple(3, "大签", LARGE_MAX_BET)
            else -> return ActionResult.EndWithMessage("猜签类型错误!")
        }

        if (draft.money <= 0 || draft.money > maxBet) {
            return ActionResult.EndWithMessage("你投注的金额不属于这个签!")
        }

        context.put(
            LotteryBet::class,
            LotteryBet(
                type = type,
                typeName = typeName,
                number = draft.number.toCharArray().joinToString(","),
                money = draft.money,
            ),
        )
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：本群还没开猜签才允许开启。
     *
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "game.lottery.open", order = 10)
    suspend fun validateNotEnabled(
        @FromProvider(LotteryProvider.ACTOR) actor: LotteryActor,
    ): ActionResult {
        val group = UserUtil.group(actor.subject.id)
        return if (PermUtil.checkUserHasPerm(group, EconPerm.LOTTERY_PERM)) {
            ActionResult.EndWithMessage("本群的猜签已经开启了!")
        } else {
            ActionResult.Continue
        }
    }

    /**
     * 前置规则（order=10）：只有已开启的群才允许关闭。
     *
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "game.lottery.close", order = 10)
    suspend fun validateEnabled(
        @FromProvider(LotteryProvider.ACTOR) actor: LotteryActor,
    ): ActionResult {
        val group = UserUtil.group(actor.subject.id)
        return if (PermUtil.checkUserHasPerm(group, EconPerm.LOTTERY_PERM)) {
            ActionResult.Continue
        } else {
            ActionResult.EndWithMessage("本群的猜签已经关闭!")
        }
    }
}

/**
 * 猜签第一步的解析结果：原始号码 + 投注金额。签种尚未判定。
 *
 * @property number 用户输入的号码原文（例如 `123`）。
 * @property money 已解析的投注金额。
 */
data class LotteryBetDraft(
    val number: String,
    val money: Double,
)

/**
 * 一次合法的猜签投注。
 *
 * 由 [LotteryRules.resolveBetType] 产出，[LotteryUsecase.bet] 直接落库，动作里不再解析。
 *
 * @property type 签种编号：1 小签 / 2 中签 / 3 大签。
 * @property typeName 签种中文名（回复用）。
 * @property number 逗号分隔的号码（`1,2,3`），与开签比较口径一致。
 * @property money 投注金额。
 */
data class LotteryBet(
    val type: Int,
    val typeName: String,
    val number: String,
    val money: Double,
)
