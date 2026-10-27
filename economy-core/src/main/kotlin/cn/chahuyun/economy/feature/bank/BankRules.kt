package cn.chahuyun.economy.feature.bank

import cn.chahuyun.economy.actionchain.annotation.After
import cn.chahuyun.economy.actionchain.annotation.Before
import cn.chahuyun.economy.actionchain.annotation.FromProvider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.actionchain.runtime.ActionResult
import cn.chahuyun.economy.feature.privatebank.PrivateBankManager
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MoneyFormatUtil

/**
 * 存款请求的归一化结果：金额 + 已经解析好的目标账户。
 *
 * 由 [BankRules.validateDeposit] 产出。路由只有 [BankRoute.MAIN] 与 [BankRoute.PRIVATE] 两种——
 * 「默认银行」在规则里就已经解析成具体目标，动作不需要再判断第三态。
 *
 * @property amount 实际存入金额（已确认 > 0 且不超过钱包余额）。
 * @property route 目标账户类型。
 * @property bankKey 私银 code；[route] 为 [BankRoute.MAIN] 时为 `null`。
 */
data class BankDepositPlan(
    val amount: Double,
    val route: BankRoute,
    val bankKey: String?,
)

/**
 * 取款请求的归一化结果，字段含义同 [BankDepositPlan]。
 *
 * @property amount 实际取出金额；私银路由下为 `null` 表示「取出该行全部存款」。
 * @property route 目标账户类型。
 * @property bankKey 私银 code；[route] 为 [BankRoute.MAIN] 时为 `null`。
 */
data class BankWithdrawPlan(
    val amount: Double?,
    val route: BankRoute,
    val bankKey: String?,
)

/**
 * 主银行功能的规则链：`@Before` / `@After` 规则集合。
 *
 * ## core.bank.deposit / core.bank.withdraw 的执行顺序
 * ```
 * @Before(order=10)  validateDeposit / validateWithdraw   解析路由与金额 → 归一化成 MAIN / PRIVATE
 * ------------------ BankUsecase.deposit / withdrawal ------------------  只做转移与回复
 * @After(order=10)   logDeposit / logWithdraw             留痕
 * ```
 *
 * 之所以把「默认银行解析」放进规则而不是动作：它是纯粹的目标判定，
 * 放前面才能在扣款前就把「目标不存在」「余额不够」全部拦下来。
 */
object BankRules {

    /**
     * 前置规则（order=10）：解析存款指令。
     *
     * 三种写法统一处理：`存款!`（默认银行全存）、`存款!!`（主银行全存）、`存款 <金额> [银行|main]`。
     *
     * @param context 本次调用上下文，用于放入 [BankDepositPlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.bank.deposit", order = 10)
    suspend fun validateDeposit(
        context: ActionContext,
        @FromProvider("bank-actor") actor: BankActor,
    ): ActionResult {
        val request = BankTransferParser.parse(actor.content, "存款", "deposit")
            ?: return ActionResult.EndWithMessage("用法：存款! / 存款!! / 存款 <金额> [银行|main]")

        val amount = request.amount ?: EconomyUtil.getMoneyByUser(actor.user)
        if (amount <= 0) {
            return ActionResult.EndWithMessage("你的钱包没有可存入的金币")
        }

        val wallet = EconomyUtil.getMoneyByUser(actor.user)
        if (wallet + 1e-6 < amount) {
            return ActionResult.EndWithMessage("你的金币不够 ${MoneyFormatUtil.format(amount)}")
        }

        val plan = when (request.route) {
            BankRoute.MAIN -> BankDepositPlan(amount, BankRoute.MAIN, null)
            BankRoute.PRIVATE -> BankDepositPlan(amount, BankRoute.PRIVATE, request.bankKey.orEmpty())
            BankRoute.DEFAULT -> {
                val defaultPb = BankUsecase.defaultPrivateBankKey(actor.userInfo.defaultPrivateBankCode)
                if (defaultPb == null) {
                    BankDepositPlan(amount, BankRoute.MAIN, null)
                } else {
                    BankDepositPlan(amount, BankRoute.PRIVATE, defaultPb)
                }
            }
        }

        context.put(BankDepositPlan::class, plan)
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：解析取款指令。
     *
     * 与存款对称，额外做两件事：
     * - 主银行路由先校验余额，避免动作里出现「先回一句失败再返回」；
     * - 私银路由先确认银行存在，并把「不填金额」保留为空（动作里按该行存款全额取）。
     *
     * @param context 本次调用上下文，用于放入 [BankWithdrawPlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.bank.withdraw", order = 10)
    suspend fun validateWithdraw(
        context: ActionContext,
        @FromProvider("bank-actor") actor: BankActor,
    ): ActionResult {
        val request = BankTransferParser.parse(actor.content, "取款", "withdraw")
            ?: return ActionResult.EndWithMessage("用法：取款! / 取款!! / 取款 <金额> [银行|main]")

        val route = when (request.route) {
            BankRoute.DEFAULT -> {
                val defaultPb = BankUsecase.defaultPrivateBankKey(actor.userInfo.defaultPrivateBankCode)
                if (defaultPb == null) BankRoute.MAIN else BankRoute.PRIVATE
            }
            else -> request.route
        }

        val bankKey = if (route == BankRoute.PRIVATE) {
            request.bankKey?.takeIf { it.isNotBlank() }
                ?: BankUsecase.defaultPrivateBankKey(actor.userInfo.defaultPrivateBankCode)
                ?: return ActionResult.EndWithMessage("用法：取款! / 取款!! / 取款 <金额> [银行|main]")
        } else {
            null
        }

        if (route == BankRoute.MAIN) {
            val amount = request.amount ?: EconomyUtil.getMoneyByBank(actor.user)
            if (amount <= 0) {
                return ActionResult.EndWithMessage("你的主银行没有可取出的金币")
            }
            if (EconomyUtil.getMoneyByBank(actor.user) + 1e-6 < amount) {
                return ActionResult.EndWithMessage("你的银行余额不够 ${MoneyFormatUtil.format(amount)} 枚金币")
            }
            context.put(BankWithdrawPlan::class, BankWithdrawPlan(amount, BankRoute.MAIN, null))
            return ActionResult.Continue
        }

        val bank = PrivateBankManager.getBank(bankKey!!)
            ?: return ActionResult.EndWithMessage("未找到该银行：$bankKey")
        if (request.amount != null && request.amount <= 0) {
            return ActionResult.EndWithMessage("你在该银行没有可取出的存款")
        }
        context.put(BankWithdrawPlan::class, BankWithdrawPlan(request.amount, BankRoute.PRIVATE, bank.code))
        return ActionResult.Continue
    }

    /**
     * 后置规则（order=10）：存款留痕。
     *
     * @param context 本次调用上下文。
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.bank.deposit", order = 10)
    suspend fun logDeposit(
        context: ActionContext,
        @FromProvider("bank-actor") actor: BankActor,
    ) {
        val plan = context.get(BankDepositPlan::class) ?: return
        Log.info(
            "银行管理:存款 用户(${actor.user.id}) 金额(${MoneyFormatUtil.format(plan.amount)}) " +
                "目标(${plan.bankKey ?: "主银行"})"
        )
    }

    /**
     * 后置规则（order=10）：取款留痕。
     *
     * @param context 本次调用上下文。
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.bank.withdraw", order = 10)
    suspend fun logWithdraw(
        context: ActionContext,
        @FromProvider("bank-actor") actor: BankActor,
    ) {
        val plan = context.get(BankWithdrawPlan::class) ?: return
        Log.info(
            "银行管理:取款 用户(${actor.user.id}) 金额(${plan.amount?.let(MoneyFormatUtil::format) ?: "全额"}) " +
                "目标(${plan.bankKey ?: "主银行"})"
        )
    }

    /**
     * 后置规则（order=10）：银行利率查询留痕。
     *
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.bank.rate", order = 10)
    suspend fun logRate(
        @FromProvider("bank-actor") actor: BankActor,
    ) {
        Log.info("银行管理:利率查询 用户(${actor.user.id})")
    }

    /**
     * 后置规则（order=10）：富豪榜查询留痕（榜单会打全服转发，留痕用于排查误触）。
     *
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.bank.regalTop", order = 10)
    suspend fun logRegalTop(
        @FromProvider("bank-actor") actor: BankActor,
    ) {
        Log.info("银行管理:富豪榜 用户(${actor.user.id})")
    }
}
