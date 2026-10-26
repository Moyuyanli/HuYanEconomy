package cn.chahuyun.economy.feature.transfer

import cn.chahuyun.economy.actionchain.annotation.Action
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MoneyFormatUtil
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageChainBuilder

/**
 * 转账功能的主用例：用户互转 + 管理员加币。
 *
 * ## 动作清单
 * | actionId | 指令 | 规则链 |
 * |---|---|---|
 * | `core.transfer.user` | 转账 [@用户] 金额 [备注] | `@Before(5)` 贷款校验 → `@Before(10)` 参数与目标校验；`@After` 转账日志 |
 * | `core.transfer.cheat` | greedisgood 金额（owner） | `@After` 加币审计 |
 *
 * 参数解析与拦截全部前置：动作只负责「余额转移 + 回复」，出问题一定能定位到规则而不是流水。
 * 原 `TransferManager` 零调用方且指向已废弃的 `Bank.INSTANCE.execute(Transfer)` 老链路，迁移时删除。
 */
object TransferUsecase {

    /**
     * 用户向另一个用户转账（`core.transfer.user`）。
     *
     * 目标与金额由 [TransferRules.validateTransfer] 解析并放进上下文。
     *
     * @param context 本次调用上下文，读取 [TransferIntent] 与参与者快照。
     */
    @Action("core.transfer.user")
    suspend fun userToUser(event: MessageEvent, context: ActionContext) {
        // 事件本身只用于来源日志；真正的转账目标与金额由前置规则解析进 TransferIntent。
        Log.info("转账指令:用户(${event.sender.id})")

        val actor = context.provide(TransferActor::class, TransferProvider.ACTOR)
        val intent = context.get(TransferIntent::class)
        if (intent == null) {
            Log.warning("转账:前置规则未产出转账意向，本次终止（用户 ${actor.user.id}）")
            return
        }

        val subject: Contact = actor.subject
        val chainBuilder = MessageChainBuilder()
        if (EconomyUtil.turnUserToUser(actor.user, intent.target, intent.money)) {
            chainBuilder.append("成功向${intent.target.nick}转账${MoneyFormatUtil.format(intent.money)}金币")
            subject.sendMessage(chainBuilder.build())
        } else {
            subject.sendMessage("转账失败！请联系管理员!")
            Log.error("转账管理:用户金币转移失败")
        }
    }

    /**
     * 管理员给自己加金币（`core.transfer.cheat`，入口已限 owner 权限）。
     *
     * @param event 消息事件。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.transfer.cheat")
    suspend fun cheat(event: MessageEvent, context: ActionContext) {
        Log.info("作弊指令:用户(${event.sender.id})")

        val actor = context.provide(TransferActor::class, TransferProvider.ACTOR)
        val subject: Contact = actor.subject
        val message: MessageChain = actor.message
        val user: User = actor.user
        val code = message.serializeToMiraiCode()

        val s = code.split(" ")
        val money = MoneyFormatUtil.parse(s.last()) ?: run {
            subject.sendMessage("作弊失败！")
            return
        }

        val chainBuilder = MessageChainBuilder()
        if (EconomyUtil.Cheat(user, money)) {
            chainBuilder.append("成功作弊: 获取${MoneyFormatUtil.format(money)}金币")
            subject.sendMessage(chainBuilder.build())
        } else {
            subject.sendMessage("转账失败！请联系管理员!")
            Log.error("转账管理:用户金币转移失败")
        }
    }
}

/**
 * 一次转账的解析结果：谁、转多少、转给谁。
 *
 * 由 [TransferRules.validateTransfer] 产出，[TransferUsecase.userToUser] 直接消费，
 * 避免动作里再做一遍 `@用户` 提取与金额解析。
 *
 * @property target 群内的目标成员（已确认存在）。
 * @property money 转账金额（已解析、已校验为正数）。
 */
data class TransferIntent(
    val target: net.mamoe.mirai.contact.Member,
    val money: Double,
)
