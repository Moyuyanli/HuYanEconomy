package cn.chahuyun.economy.feature.privatebank

import cn.chahuyun.economy.actionchain.annotation.After
import cn.chahuyun.economy.actionchain.annotation.Before
import cn.chahuyun.economy.actionchain.annotation.FromProvider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.actionchain.runtime.ActionResult
import cn.chahuyun.economy.data.repository.PrivateBankRepository
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MoneyFormatUtil
import net.mamoe.mirai.contact.Group

// ---------------------------------------------------------------------------
// 规则间传递的中间结果
//
// 每个 data class 对应「一个动作解析出的、动作本体要用的东西」。
// 全部由 @Before 规则产出、动作消费，动作里不再重复解析指令文本。
// ---------------------------------------------------------------------------

/**
 * 银行创建参数。
 *
 * @property code 银行唯一 code。
 * @property name 银行显示名。
 */
data class PrivateBankCreatePlan(val code: String, val name: String)

/**
 * 「操作者自己作为行长的银行」快照，被 6 个动作共用。
 *
 * @property bankCode 银行 code。
 * @property bankName 银行名称（回复与标题用）。
 */
data class PrivateBankOwnedBank(val bankCode: String, val bankName: String)

/**
 * 资金池操作参数。
 *
 * @property amount 金额（已确认 > 0）。
 * @property pool 补资/撤资的目标池；`null` 表示这是池间划转。
 * @property transfer 池间划转方向；`null` 表示这是单向补资/撤资。
 */
data class PrivateBankPoolPlan(
    val amount: Double,
    val pool: PrivateBankService.FundingPool?,
    val transfer: PrivateBankService.FundingPoolTransfer?,
)

/**
 * 银行资料修改参数。
 *
 * @property code 目标银行 code。
 * @property name 新名称。
 */
data class PrivateBankProfilePlan(val code: String, val name: String)

/**
 * 发布放贷额度参数。
 *
 * @property amount 放贷额度（已确认 > 0）。
 * @property ratePercent 日利率（百分数，已按「>10 视为千分比」归一化）。
 * @property bankCode 行长银行 code。
 */
data class PrivateBankOfferPlan(val amount: Double, val ratePercent: Double, val bankCode: String)

/**
 * 只带 offerId 的参数（撤贷）。
 *
 * @property offerId 放贷项目 id。
 */
data class PrivateBankOfferIdPlan(val offerId: Int)

/**
 * 修改放贷利率参数。
 *
 * @property offerId 放贷项目 id。
 * @property rate 新日利率（百分数）。
 */
data class PrivateBankOfferRatePlan(val offerId: Int, val rate: Double)

/**
 * 「银行信息」的查询目标。
 *
 * @property bankKey 已经归一化成具体 code 的银行标识。
 */
data class PrivateBankInfoTarget(val bankKey: String)

/**
 * 默认银行设置结果。
 *
 * @property bankKey 目标银行 code；空串表示切回主银行。
 * @property bankName 目标银行名称（回复用）。
 */
data class PrivateBankDefaultPlan(val bankKey: String, val bankName: String)

/**
 * 银行评分参数。
 *
 * @property rating 1–5 的星级。
 * @property bankKey 被评分银行 code。
 * @property content 评价内容，可为 `null`。
 */
data class PrivateBankReviewPlan(val rating: Int, val bankKey: String, val content: String?)

/**
 * 借贷/还款参数。
 *
 * @property amount 金额（借贷时已确认 > 0）。
 * @property bankKey 目标银行 code。
 */
data class PrivateBankDebtPlan(val amount: Double, val bankKey: String)

/**
 * 狐卷竞标参数。
 *
 * @property code 狐卷 code。
 * @property premium 溢价金额。
 * @property rate 接受的日利率（百分数）。
 */
data class PrivateBankFoxBidPlan(val code: String, val premium: Double, val rate: Double)

/**
 * 私银功能的规则链：`@Before` / `@After` 规则集合。
 *
 * ## 设计口径
 * - **行长身份判定只有一处**：`你还没有创建自己的银行` 由 [requireOwnedBank] 统一产出，
 *   9 个需要行长身份的动作共用，避免每个动作各写一遍。
 * - **参数解析全在 `@Before(10)`**：格式不对直接结束，动作拿不到半成品参数。
 * - **`@After(10)` 只做留痕**：私银的资金变动由 `PrivateBankLedger` 记账，日志用于人工对账。
 */
object PrivateBankRules {

    // -----------------------------------------------------------------------
    // 前置规则
    // -----------------------------------------------------------------------

    /**
     * 前置规则（order=10）：解析 `银行创建 <code> <name>`。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankCreatePlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.create", order = 10)
    suspend fun validateCreate(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val parts = actor.content.trim().split(" ", limit = 3)
        if (parts.size < 3) {
            return ActionResult.EndWithMessage("用法：银行创建 <code> <name>")
        }
        val code = parts[1].trim()
        val name = parts[2].trim()
        if (code.isBlank() || name.isBlank()) {
            return ActionResult.EndWithMessage("用法：银行创建 <code> <name>")
        }
        context.put(PrivateBankCreatePlan::class, PrivateBankCreatePlan(code, name))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：`银行描述修改` 需要行长身份 + 未破产 + 群聊。
     *
     * 描述是通过「下一条群消息」收集的，所以群聊是硬条件，不能等到动作里才发现。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankOwnedBank]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.desc", order = 10)
    suspend fun validateDesc(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val owned = ownedBankOf(actor) ?: return ActionResult.EndWithMessage("你还没有创建自己的银行")
        if (PrivateBankRepository.findBankByCode(owned.bankCode)?.isBankrupt() == true) {
            return ActionResult.EndWithMessage("银行已经破产，不能修改描述")
        }
        if (actor.subject !is Group) {
            return ActionResult.EndWithMessage("请在群聊中使用该指令")
        }
        context.put(PrivateBankOwnedBank::class, owned)
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：`银行利率变更` 需要行长身份。
     *
     * 利率数值本身的合法性由 `PrivateBankService.setDepositorInterest` 校验（区间会随星级变化）。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankOwnedBank]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.rate", order = 10)
    suspend fun validateRate(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val owned = ownedBankOf(actor) ?: return ActionResult.EndWithMessage("你还没有创建自己的银行")
        context.put(PrivateBankOwnedBank::class, owned)
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：解析 `银行补资 <金额> [P|F|PF|FP]`。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankPoolPlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.fund", order = 10)
    suspend fun validateFund(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val parts = actor.content.trim().split(Regex("\\s+"))
        val amount = parts.getOrNull(1)?.let(MoneyFormatUtil::parse)
        val poolToken = parts.getOrNull(2)
        val transfer = PrivateBankService.parseFundingPoolTransfer(poolToken)
        val pool = if (transfer == null) PrivateBankService.FundingPool.parse(poolToken) else null
        if (amount == null || amount <= 0 || (transfer == null && pool == null)) {
            return ActionResult.EndWithMessage("用法：银行补资 <金额> [P|F|PF|FP]，PF=准备金池转流动金池")
        }
        context.put(PrivateBankPoolPlan::class, PrivateBankPoolPlan(amount, pool, transfer))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：解析 `银行撤资 <金额> [P|F]`。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankPoolPlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.divest", order = 10)
    suspend fun validateDivest(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val parts = actor.content.trim().split(Regex("\\s+"))
        val amount = parts.getOrNull(1)?.let(MoneyFormatUtil::parse)
        val pool = PrivateBankService.FundingPool.parse(parts.getOrNull(2))
        if (amount == null || amount <= 0 || pool == null) {
            return ActionResult.EndWithMessage("用法：银行撤资 <金额> [P|F]，P=准备金池，F=流动金池")
        }
        context.put(PrivateBankPoolPlan::class, PrivateBankPoolPlan(amount, pool, null))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：解析 `银行资料修改 <code> <name>`。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankProfilePlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.profile", order = 10)
    suspend fun validateProfile(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val parts = actor.content.trim().split(Regex("\\s+"), limit = 3)
        val code = parts.getOrNull(1)
        val name = parts.getOrNull(2)
        if (code.isNullOrBlank() || name.isNullOrBlank()) {
            return ActionResult.EndWithMessage("用法：银行资料修改 <code> <name>")
        }
        context.put(PrivateBankProfilePlan::class, PrivateBankProfilePlan(code, name))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：解析 `(银行)放贷 <金额> [利率]`，并做 5 秒去重。
     *
     * 去重放在规则里：命中时动作根本不会执行，不会出现「扣了额度才发现重复」的情况。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankOfferPlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.loan.offer", order = 10)
    suspend fun validateLoanOffer(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val parts = actor.content.trim().split(Regex("\\s+"))
        val money = parts.getOrNull(1)?.let(MoneyFormatUtil::parse)
        if (money == null || money <= 0) {
            return ActionResult.EndWithMessage(
                "用法：放贷 <金额> [利率]，例如：放贷 100000 / 放贷 10M / 放贷 20K"
            )
        }

        val owned = ownedBankOf(actor) ?: return ActionResult.EndWithMessage("你还没有创建自己的银行")

        val dedupKey = listOf(actor.sender.id, owned.bankCode, money, actor.content.trim()).joinToString("|")
        if (!PrivateBankUsecase.markLoanOfferCommandIfFresh(dedupKey)) {
            return ActionResult.EndWithMessage("放贷请求已收到，请勿重复提交")
        }

        val bank = PrivateBankRepository.findBankByCode(owned.bankCode)
        val defaultRate = (bank?.depositorInterest ?: 0) / 10.0
        val rateRaw = parts.getOrNull(2)?.toDoubleOrNull() ?: defaultRate
        val ratePercent = if (rateRaw > 10) rateRaw / 10.0 else rateRaw

        context.put(PrivateBankOfferPlan::class, PrivateBankOfferPlan(money, ratePercent, owned.bankCode))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：解析 `贷款利息修改 <offerId> <1.0-18.0>`。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankOfferRatePlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.loan.rate", order = 10)
    suspend fun validateLoanRate(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val parts = actor.content.trim().split(Regex("\\s+"))
        val offerId = parts.getOrNull(1)?.toIntOrNull()
        val rate = parts.getOrNull(2)?.toDoubleOrNull()
        if (offerId == null || rate == null) {
            return ActionResult.EndWithMessage("用法：贷款利息修改 <offerId> <1.0-18.0>")
        }
        context.put(PrivateBankOfferRatePlan::class, PrivateBankOfferRatePlan(offerId, rate))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：`放贷列表` 需要行长身份。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankOwnedBank]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.loan.list", order = 10)
    suspend fun validateLoanList(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val owned = ownedBankOf(actor) ?: return ActionResult.EndWithMessage("你还没有创建自己的银行")
        context.put(PrivateBankOwnedBank::class, owned)
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：解析 `撤贷 <offerId>`。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankOfferIdPlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.loan.cancel", order = 10)
    suspend fun validateLoanCancel(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val offerId = actor.content.trim().split(Regex("\\s+")).getOrNull(1)?.toIntOrNull()
            ?: return ActionResult.EndWithMessage("用法：撤贷 <offerId>")
        context.put(PrivateBankOfferIdPlan::class, PrivateBankOfferIdPlan(offerId))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：解析 `狐卷竞标 <code> <溢价金额> <接受利息>`。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankFoxBidPlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.fox.bid", order = 10)
    suspend fun validateFoxBid(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val parts = actor.content.trim().split(" ")
        if (parts.size < 4) {
            return ActionResult.EndWithMessage("用法：狐卷竞标 <code> <溢价金额> <接受利息(%/day)>")
        }
        val plan = PrivateBankFoxBidPlan(
            code = parts[1],
            premium = MoneyFormatUtil.parse(parts[2]) ?: 0.0,
            rate = parts[3].toDoubleOrNull() ?: 0.0,
        )
        context.put(PrivateBankFoxBidPlan::class, plan)
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：确定 `银行信息` 要看哪家银行。
     *
     * 三级回退：显式参数 → 自己拥有的银行 → 默认私银；都没有就提示。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankInfoTarget]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.info", order = 10)
    suspend fun validateInfo(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val arg = actor.content.trim().split(" ").getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        if (arg != null) {
            context.put(PrivateBankInfoTarget::class, PrivateBankInfoTarget(arg))
            return ActionResult.Continue
        }

        val ownerBank = ownedBankOf(actor)
        val key = ownerBank?.bankCode ?: actor.userInfo.defaultPrivateBankCode.trim().takeIf { it.isNotBlank() }
        if (key.isNullOrBlank()) {
            return ActionResult.EndWithMessage("你还没有创建自己的银行，也没有默认银行")
        }
        context.put(PrivateBankInfoTarget::class, PrivateBankInfoTarget(key))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：`我的银行` 需要行长身份。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankOwnedBank]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.my", order = 10)
    suspend fun validateMyBank(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val owned = ownedBankOf(actor) ?: return ActionResult.EndWithMessage("你没有创建银行")
        context.put(PrivateBankOwnedBank::class, owned)
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：`我的银行#` 需要行长身份。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankOwnedBank]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.myText", order = 10)
    suspend fun validateMyBankText(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val owned = ownedBankOf(actor) ?: return ActionResult.EndWithMessage("你没有创建银行")
        context.put(PrivateBankOwnedBank::class, owned)
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：解析 `银行评分 <1-5> <描述> [code/name]`。
     *
     * 末段能被识别成银行就当成银行 code，否则整段都算描述——这个歧义解析放规则里，
     * 动作只消费结果。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankReviewPlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.review", order = 10)
    suspend fun validateReview(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val parts = actor.content.trim().split(" ")
        val rating = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val rest = parts.drop(2)

        var bankKey: String? = null
        var content: String? = null

        if (rest.isNotEmpty()) {
            val last = rest.last()
            val bank = PrivateBankService.getBank(last)
            if (bank != null) {
                bankKey = bank.code
                content = rest.dropLast(1).joinToString(" ").trim().takeIf { it.isNotBlank() }
            } else {
                content = rest.joinToString(" ").trim().takeIf { it.isNotBlank() }
            }
        }
        if (bankKey.isNullOrBlank()) bankKey = actor.userInfo.defaultPrivateBankCode
        if (bankKey.isNullOrBlank()) {
            return ActionResult.EndWithMessage("请指定银行：银行评分 <1-5> <描述> [code/name]")
        }

        context.put(PrivateBankReviewPlan::class, PrivateBankReviewPlan(rating, bankKey, content))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：解析 `(贷款|借款) <金额> [code/name]`。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankDebtPlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.borrow", order = 10)
    suspend fun validateBorrow(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val parts = actor.content.trim().split(" ")
        val amount = parts.getOrNull(1)?.let(MoneyFormatUtil::parse) ?: 0.0
        val key = parts.getOrNull(2) ?: actor.userInfo.defaultPrivateBankCode
        if (key.isNullOrBlank()) {
            return ActionResult.EndWithMessage("请指定银行：贷款 <金额> [code/name]")
        }
        context.put(PrivateBankDebtPlan::class, PrivateBankDebtPlan(amount, key))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：解析 `还款 <金额> [code/name]`。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankDebtPlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.repay", order = 10)
    suspend fun validateRepay(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val parts = actor.content.trim().split(" ")
        val amount = parts.getOrNull(1)?.let(MoneyFormatUtil::parse) ?: 0.0
        val key = parts.getOrNull(2) ?: actor.userInfo.defaultPrivateBankCode
        if (key.isNullOrBlank()) {
            return ActionResult.EndWithMessage("请指定银行：还款 <金额> [code/name]")
        }
        context.put(PrivateBankDebtPlan::class, PrivateBankDebtPlan(amount, key))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：解析 `默认银行设置 <code/name|main|主银行>`。
     *
     * `main` / `主银行` 归一化成空串（= 切回主银行），目标银行不存在或已破产直接拦截，
     * 保证动作里只有「写回用户资料」一件事。
     *
     * @param context 本次调用上下文，用于放入 [PrivateBankDefaultPlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.privatebank.default.set", order = 10)
    suspend fun validateDefaultSet(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ): ActionResult {
        val key = actor.content.trim().split(Regex("\\s+"), limit = 2).getOrNull(1)?.trim().orEmpty()
        if (key.isBlank()) {
            return ActionResult.EndWithMessage("用法：默认银行设置 <code/name|main|主银行>")
        }

        if (key.equals("main", ignoreCase = true) || key == "主银行") {
            context.put(PrivateBankDefaultPlan::class, PrivateBankDefaultPlan("", "主银行"))
            return ActionResult.Continue
        }

        val bank = PrivateBankService.getBank(key)
            ?: return ActionResult.EndWithMessage("未找到该银行：$key")
        if (bank.isBankrupt()) {
            return ActionResult.EndWithMessage("该银行已经破产，不能设为默认银行")
        }
        context.put(PrivateBankDefaultPlan::class, PrivateBankDefaultPlan(bank.code, bank.name))
        return ActionResult.Continue
    }

    // -----------------------------------------------------------------------
    // 后置规则
    // -----------------------------------------------------------------------

    /**
     * 后置规则（order=10）：开户留痕。
     *
     * @param context 本次调用上下文。
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.privatebank.create", order = 10)
    suspend fun logCreate(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ) {
        val plan = context.get(PrivateBankCreatePlan::class) ?: return
        Log.info("银行:开户 用户(${actor.sender.id}) code(${plan.code}) name(${plan.name})")
    }

    /**
     * 后置规则（order=10）：资金池变动留痕。
     *
     * @param context 本次调用上下文。
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.privatebank.fund", order = 10)
    suspend fun logFund(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ) {
        val plan = context.get(PrivateBankPoolPlan::class) ?: return
        Log.info(
            "银行:补资 用户(${actor.sender.id}) 金额(${MoneyFormatUtil.format(plan.amount)}) " +
                "池(${plan.transfer?.let { "${it.from.code}->${it.to.code}" } ?: plan.pool?.displayName})"
        )
    }

    /**
     * 后置规则（order=10）：撤资留痕。
     *
     * @param context 本次调用上下文。
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.privatebank.divest", order = 10)
    suspend fun logDivest(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ) {
        val plan = context.get(PrivateBankPoolPlan::class) ?: return
        Log.info(
            "银行:撤资 用户(${actor.sender.id}) 金额(${MoneyFormatUtil.format(plan.amount)}) " +
                "池(${plan.pool?.displayName})"
        )
    }

    /**
     * 后置规则（order=10）：放贷额度留痕。
     *
     * @param context 本次调用上下文。
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.privatebank.loan.offer", order = 10)
    suspend fun logLoanOffer(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ) {
        val plan = context.get(PrivateBankOfferPlan::class) ?: return
        Log.info(
            "银行:放贷 用户(${actor.sender.id}) 银行(${plan.bankCode}) " +
                "额度(${MoneyFormatUtil.format(plan.amount)}) 利率(${plan.ratePercent}%/day)"
        )
    }

    /**
     * 后置规则（order=10）：放贷利率修改留痕。
     *
     * @param context 本次调用上下文。
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.privatebank.loan.rate", order = 10)
    suspend fun logLoanRate(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ) {
        val plan = context.get(PrivateBankOfferRatePlan::class) ?: return
        Log.info("银行:改息 用户(${actor.sender.id}) offer(${plan.offerId}) 利率(${plan.rate}%/day)")
    }

    /**
     * 后置规则（order=10）：借贷留痕。
     *
     * @param context 本次调用上下文。
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.privatebank.borrow", order = 10)
    suspend fun logBorrow(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ) {
        val plan = context.get(PrivateBankDebtPlan::class) ?: return
        Log.info(
            "银行:借贷 用户(${actor.sender.id}) 银行(${plan.bankKey}) 金额(${MoneyFormatUtil.format(plan.amount)})"
        )
    }

    /**
     * 后置规则（order=10）：还款留痕。
     *
     * @param context 本次调用上下文。
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.privatebank.repay", order = 10)
    suspend fun logRepay(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ) {
        val plan = context.get(PrivateBankDebtPlan::class) ?: return
        Log.info(
            "银行:还款 用户(${actor.sender.id}) 银行(${plan.bankKey}) 金额(${MoneyFormatUtil.format(plan.amount)})"
        )
    }

    /**
     * 后置规则（order=10）：狐卷竞标留痕。
     *
     * @param context 本次调用上下文。
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.privatebank.fox.bid", order = 10)
    suspend fun logFoxBid(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ) {
        val plan = context.get(PrivateBankFoxBidPlan::class) ?: return
        Log.info(
            "银行:狐卷竞标 用户(${actor.sender.id}) code(${plan.code}) " +
                "溢价(${MoneyFormatUtil.format(plan.premium)}) 利率(${plan.rate}%/day)"
        )
    }

    /**
     * 后置规则（order=10）：默认银行切换留痕。
     *
     * @param context 本次调用上下文。
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.privatebank.default.set", order = 10)
    suspend fun logDefaultSet(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ) {
        val plan = context.get(PrivateBankDefaultPlan::class) ?: return
        Log.info("银行:默认切换 用户(${actor.sender.id}) 目标(${plan.bankKey.ifBlank { "主银行" }})")
    }

    /**
     * 后置规则（order=10）：银行评分留痕。
     *
     * @param context 本次调用上下文。
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.privatebank.review", order = 10)
    suspend fun logReview(
        context: ActionContext,
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ) {
        val plan = context.get(PrivateBankReviewPlan::class) ?: return
        Log.info("银行:评分 用户(${actor.sender.id}) 银行(${plan.bankKey}) 星级(${plan.rating})")
    }

    /**
     * 后置规则（order=10）：狐卷补发留痕（owner 专用指令）。
     *
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.privatebank.fox.supplement", order = 10)
    suspend fun logFoxSupplement(
        @FromProvider("privatebank-actor") actor: PrivateBankActor,
    ) {
        Log.info("银行:狐卷补发 操作者(${actor.sender.id})")
    }

    // -----------------------------------------------------------------------
    // 共用判定
    // -----------------------------------------------------------------------

    /**
     * 取「操作者自己作为行长的银行」。
     *
     * @param actor 本次指令的参与者快照。
     * @return 银行 code 与名称；没有返回 `null`。
     */
    private fun ownedBankOf(actor: PrivateBankActor): PrivateBankOwnedBank? {
        val bank = PrivateBankManager.ownedBank(actor.sender.id) ?: return null
        return PrivateBankOwnedBank(bank.code, bank.name)
    }
}
