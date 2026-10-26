package cn.chahuyun.economy.feature.transfer

import cn.chahuyun.economy.actionchain.annotation.After
import cn.chahuyun.economy.actionchain.annotation.Before
import cn.chahuyun.economy.actionchain.annotation.FromProvider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.actionchain.runtime.ActionResult
import cn.chahuyun.economy.feature.privatebank.PrivateBankService
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MoneyFormatUtil
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.SingleMessage

/**
 * 转账功能的规则链：`@Before` / `@After` 规则集合。
 *
 * ## core.transfer.user 的执行顺序
 * ```
 * @Before(order=5)  validateNoUnpaidLoan  有未还贷款 → 拦截
 * @Before(order=10) validateTransfer      解析 @目标 与金额 → 目标存在？金额合法？→ 放入 TransferIntent
 * ------------------ TransferUsecase.userToUser ------------------  余额转移 + 回复
 * @After(order=10)  logTransferred        转账留痕
 * ```
 *
 * 顺序有意义：贷款校验放在解析之前，用户即使参数写错也先看到「不能转账」这个真实原因。
 */
object TransferRules {

    /**
     * 前置规则（order=5）：有未还贷款时禁止转账。
     *
     * 这是私银放贷的追缴保障——允许转账会让债务人把余额转移走、绕开自动扣缴。
     *
     * @param actor 本次转账指令的参与者快照。
     */
    @Before(actionId = "core.transfer.user", order = 5)
    suspend fun validateNoUnpaidLoan(
        @FromProvider("transfer-actor") actor: TransferActor,
    ): ActionResult {
        return if (PrivateBankService.hasUnrepaidLoans(actor.user.id)) {
            ActionResult.EndWithMessage("你还有未还贷款，暂时不能转账")
        } else {
            ActionResult.Continue
        }
    }

    /**
     * 前置规则（order=10）：解析并校验转账参数。
     *
     * 两种写法都支持：
     * - `转账 [@某人] 金额`（消息里带 At，序列化后仍是两段）；
     * - `转账 QQ号 金额`。
     *
     * 全部校验通过后把 [TransferIntent] 放进上下文；任一失败直接结束，不会发生任何扣款。
     *
     * @param context 本次调用上下文，用于放入解析结果。
     * @param actor 本次转账指令的参与者快照。
     */
    @Before(actionId = "core.transfer.user", order = 10)
    suspend fun validateTransfer(
        context: ActionContext,
        @FromProvider("transfer-actor") actor: TransferActor,
    ): ActionResult {
        val message = actor.message
        val code = message.serializeToMiraiCode()
        val segments = code.split(" ")

        var targetQq = 0L
        var money: Double?

        if (segments.size == 2) {
            // 带 At 的写法：目标从消息元素里取，金额是最后一段。
            for (singleMessage: SingleMessage in message) {
                if (singleMessage is At) {
                    targetQq = singleMessage.target
                }
            }
            money = MoneyFormatUtil.parse(segments.last())
        } else {
            // 纯 QQ 号写法：第二段必须是数字，非法时按「转账失败」处理（原实现会抛异常）。
            targetQq = segments.getOrNull(1)?.toLongOrNull() ?: return ActionResult.EndWithMessage("转账失败！")
            money = segments.getOrNull(2)?.let { MoneyFormatUtil.parse(it) }
        }

        money ?: return ActionResult.EndWithMessage("转账失败！")

        if (money < 0 || actor.user.id == targetQq) {
            return ActionResult.EndWithMessage("耍我了？小子？")
        }

        val group = actor.subject as? Group
        if (group == null || targetQq == 0L) {
            return ActionResult.EndWithMessage("转账失败！")
        }

        val member = group[targetQq] ?: return ActionResult.EndWithMessage("转账失败！")

        context.put(TransferIntent::class, TransferIntent(target = member, money = money))
        return ActionResult.Continue
    }

    /**
     * 后置规则（order=10）：转账留痕。
     *
     * 只记录意向，不判断最终是否成功——失败时同样需要能对上「谁试图转多少给谁」。
     *
     * @param context 本次调用上下文。
     * @param actor 本次转账指令的参与者快照。
     */
    @After(actionId = "core.transfer.user", order = 10)
    suspend fun logTransferred(
        context: ActionContext,
        @FromProvider("transfer-actor") actor: TransferActor,
    ) {
        val intent = context.get(TransferIntent::class) ?: return
        Log.info(
            "转账:用户(${actor.user.id}) -> (${intent.target.id}) 金额(${MoneyFormatUtil.format(intent.money)})"
        )
    }

    /**
     * 后置规则（order=10）：管理员加币审计。
     *
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.transfer.cheat", order = 10)
    suspend fun logCheat(
        @FromProvider("transfer-actor") actor: TransferActor,
    ) {
        Log.info("作弊指令:用户(${actor.user.id}) 指令(${actor.message.contentToString()})")
    }
}
