package cn.chahuyun.economy.feature.redpack

import cn.chahuyun.economy.actionchain.annotation.After
import cn.chahuyun.economy.actionchain.annotation.Before
import cn.chahuyun.economy.actionchain.annotation.FromProvider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.actionchain.runtime.ActionResult
import cn.chahuyun.economy.model.redpack.RedPackKind
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MoneyFormatUtil

/**
 * 创建红包的解析结果。
 *
 * @property money 红包总金额（已确认不超过钱包余额）。
 * @property number 红包个数。
 * @property type 均分 / 随机 / 口令。
 * @property password 口令；非口令红包为空串。
 */
data class RedPackCreatePlan(
    val money: Double,
    val number: Int,
    val type: RedPackKind,
    val password: String,
)

/**
 * 红包功能的规则链：`@Before` / `@After` 规则集合。
 *
 * ## core.redpack.create 的执行顺序
 * ```
 * @Before(order=10)  validateCreate   解析金额/个数/类型/口令 → 单价下限 → 余额校验
 * ------------------ RedPackUsecase.create ------------------  扣款 + 生成随机分配 + 落库 + 回复
 * @After(order=10)   logCreated       留痕
 * ```
 *
 * 扣款留在动作里：它是**不可回滚的资金操作**，必须和「建红包 + 落库」在同一段代码里，
 * 一旦落库失败才好就近补偿；规则只做「能不能扣」的判断。
 */
object RedPackRules {

    /** 口令合法长度：1–32 个任意字符（含换行以外的空白）。 */
    private val PASSWORD_REGEX = "^[\\s\\S]{1,32}$".toRegex()

    /**
     * 前置规则（order=10）：解析并校验发红包参数。
     *
     * 参数残缺时保持原有「静默不回复」的行为（指令正则已经保证基本形态，走到这里说明是真异常）。
     *
     * @param context 本次调用上下文，用于放入 [RedPackCreatePlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.redpack.create", order = 10)
    suspend fun validateCreate(
        context: ActionContext,
        @FromProvider("redpack-actor") actor: RedPackActor,
    ): ActionResult {
        val info = actor.content.split(" ").filter { it.isNotBlank() }
        if (info.size < 3) return ActionResult.EndSilently

        val money = MoneyFormatUtil.parse(info[1]) ?: return ActionResult.EndSilently
        val number = info[2].toIntOrNull() ?: return ActionResult.EndSilently

        var type = RedPackKind.NORMAL
        var password = ""

        if (info.size >= 4) {
            val typeStr = info[3]
            if (typeStr == "sj" || typeStr == "随机") {
                type = RedPackKind.RANDOM
            } else if (typeStr == "kl" || typeStr == "口令") {
                if (info.size < 5) {
                    @Suppress("SpellCheckingInspection")
                    val hint = "口令红包需要提供口令！例：发红包 10 5 口令 Ciallo～(∠・ω< )⌒★"
                    return ActionResult.EndWithMessage(hint)
                }
                type = RedPackKind.PASSWORD
                password = info.subList(4, info.size).joinToString(" ")
                if (!PASSWORD_REGEX.matches(password)) {
                    return ActionResult.EndWithMessage("口令不合法！必须是1~32个基本字符。")
                }
            }
        }

        if (number <= 0) return ActionResult.EndSilently
        if (money / number < 0.1) {
            return ActionResult.EndWithMessage("你发的红包太小了，每个红包金额不能低于 0.1！")
        }
        if (money > EconomyUtil.getMoneyByUser(actor.sender)) {
            return ActionResult.EndWithMessage("你的金币不够啦！")
        }

        context.put(RedPackCreatePlan::class, RedPackCreatePlan(money, number, type, password))
        return ActionResult.Continue
    }

    /**
     * 后置规则（order=10）：创建红包留痕。
     *
     * @param context 本次调用上下文。
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.redpack.create", order = 10)
    suspend fun logCreated(
        context: ActionContext,
        @FromProvider("redpack-actor") actor: RedPackActor,
    ) {
        val plan = context.get(RedPackCreatePlan::class) ?: return
        Log.info(
            "红包:创建 用户(${actor.sender.id}) 群(${actor.group?.id}) " +
                "金额(${MoneyFormatUtil.format(plan.money)}) 个数(${plan.number}) 类型(${plan.type})"
        )
    }

    /**
     * 后置规则（order=10）：一键抢红包留痕。
     *
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.redpack.grabNewest", order = 10)
    suspend fun logGrabNewest(
        @FromProvider("redpack-actor") actor: RedPackActor,
    ) {
        Log.info("红包:一键抢 用户(${actor.sender.id}) 群(${actor.group?.id})")
    }

    /**
     * 后置规则（order=10）：红包开关留痕。
     *
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.redpack.open", order = 10)
    suspend fun logOpen(
        @FromProvider("redpack-actor") actor: RedPackActor,
    ) {
        Log.info("红包:开启 操作者(${actor.sender.id}) 群(${actor.group?.id})")
    }

    /**
     * 后置规则（order=10）：红包开关留痕。
     *
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.redpack.close", order = 10)
    suspend fun logClose(
        @FromProvider("redpack-actor") actor: RedPackActor,
    ) {
        Log.info("红包:关闭 操作者(${actor.sender.id}) 群(${actor.group?.id})")
    }
}
