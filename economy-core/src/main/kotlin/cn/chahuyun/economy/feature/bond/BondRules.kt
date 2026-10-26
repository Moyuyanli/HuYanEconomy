package cn.chahuyun.economy.feature.bond

import cn.chahuyun.economy.actionchain.annotation.After
import cn.chahuyun.economy.actionchain.annotation.Before
import cn.chahuyun.economy.actionchain.annotation.FromProvider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.actionchain.runtime.ActionResult
import cn.chahuyun.economy.feature.privatebank.PrivateBankManager
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MoneyFormatUtil

/**
 * 购买国卷的解析结果。
 *
 * @property amount 购买金额（已确认 > 0）。
 * @property code 国卷 code。
 * @property bankCode 行长银行的 code（已确认存在）。
 */
data class BondBuyPlan(
    val amount: Double,
    val code: String,
    val bankCode: String,
)

/**
 * 赎回国卷的解析结果。
 *
 * @property code 国卷 code。
 * @property amount 赎回本金；`null` 表示赎回该 code 的全部持仓。
 * @property bankCode 行长银行的 code（已确认存在）。
 */
data class BondRedeemPlan(
    val code: String,
    val amount: Double?,
    val bankCode: String,
)

/**
 * 「查看某家银行的国卷持仓」的作用域：只带展示需要的 code 与名称。
 *
 * @property bankCode 行长银行 code。
 * @property bankName 行长银行名称（标题里显示）。
 */
data class BondHoldingScope(
    val bankCode: String,
    val bankName: String,
)

/**
 * 国卷功能的规则链：`@Before` / `@After` 规则集合。
 *
 * ## core.bond.buy / core.bond.redeem 的执行顺序
 * ```
 * @Before(order=10)  validateBuy / validateRedeem   解析参数 → 行长银行存在性 → 放入计划
 * ------------------ BondUsecase.buyBond / redeemBond ------------------  调私银服务 + 回复
 * @After(order=10)   logBuy / logRedeem             留痕
 * ```
 *
 * 「你还没有创建自己的银行」这条拦截被 4 个动作共用（买、赎回、持仓、查看持仓），
 * 规则化之后判定只写一次，动作不必各自重复。
 */
object BondRules {

    /**
     * 前置规则（order=10）：解析购买国卷指令。
     *
     * @param context 本次调用上下文，用于放入 [BondBuyPlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.bond.buy", order = 10)
    suspend fun validateBuy(
        context: ActionContext,
        @FromProvider("bond-actor") actor: BondActor,
    ): ActionResult {
        val parts = actor.content.trim().split(" ")
        val amount = parts.getOrNull(1)?.let(MoneyFormatUtil::parse) ?: 0.0
        val code = parts.getOrNull(2).orEmpty()
        if (amount <= 0 || code.isBlank()) {
            return ActionResult.EndWithMessage("用法：购买国卷 <金额> <code>")
        }

        val bank = PrivateBankManager.ownedBank(actor.sender.id)
            ?: return ActionResult.EndWithMessage("你还没有创建自己的银行")

        context.put(BondBuyPlan::class, BondBuyPlan(amount, code, bank.code))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：解析赎回国卷指令。
     *
     * @param context 本次调用上下文，用于放入 [BondRedeemPlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.bond.redeem", order = 10)
    suspend fun validateRedeem(
        context: ActionContext,
        @FromProvider("bond-actor") actor: BondActor,
    ): ActionResult {
        val parts = actor.content.trim().split(" ")
        val code = parts.getOrNull(1).orEmpty()
        val amount = parts.getOrNull(2)?.let(MoneyFormatUtil::parse)
        if (code.isBlank()) {
            return ActionResult.EndWithMessage("用法：赎回国卷 <code> [金额]")
        }

        val bank = PrivateBankManager.ownedBank(actor.sender.id)
            ?: return ActionResult.EndWithMessage("你还没有创建自己的银行")

        context.put(BondRedeemPlan::class, BondRedeemPlan(code, amount, bank.code))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：查看持仓前先确认行长身份。
     *
     * @param context 本次调用上下文，用于放入行长银行 code。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.bond.holdings", order = 10)
    suspend fun validateHoldings(
        context: ActionContext,
        @FromProvider("bond-actor") actor: BondActor,
    ): ActionResult {
        val bank = PrivateBankManager.ownedBank(actor.sender.id)
            ?: return ActionResult.EndWithMessage("你还没有创建自己的银行")

        context.put(BondHoldingScope::class, BondHoldingScope(bank.code, bank.name))
        return ActionResult.Continue
    }

    /**
     * 后置规则（order=10）：购买国卷留痕。
     *
     * @param context 本次调用上下文。
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.bond.buy", order = 10)
    suspend fun logBuy(
        context: ActionContext,
        @FromProvider("bond-actor") actor: BondActor,
    ) {
        val plan = context.get(BondBuyPlan::class) ?: return
        Log.info(
            "国卷:购买 用户(${actor.sender.id}) 银行(${plan.bankCode}) " +
                "code(${plan.code}) 金额(${MoneyFormatUtil.format(plan.amount)})"
        )
    }

    /**
     * 后置规则（order=10）：赎回国卷留痕。
     *
     * @param context 本次调用上下文。
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.bond.redeem", order = 10)
    suspend fun logRedeem(
        context: ActionContext,
        @FromProvider("bond-actor") actor: BondActor,
    ) {
        val plan = context.get(BondRedeemPlan::class) ?: return
        Log.info(
            "国卷:赎回 用户(${actor.sender.id}) 银行(${plan.bankCode}) " +
                "code(${plan.code}) 金额(${plan.amount?.let(MoneyFormatUtil::format) ?: "全额"})"
        )
    }

    /**
     * 后置规则（order=10）：国卷补发留痕（owner 专用指令）。
     *
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.bond.supplement", order = 10)
    suspend fun logSupplement(
        @FromProvider("bond-actor") actor: BondActor,
    ) {
        Log.info("国卷:补发 操作者(${actor.sender.id})")
    }
}
