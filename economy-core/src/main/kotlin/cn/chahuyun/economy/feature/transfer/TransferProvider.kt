package cn.chahuyun.economy.feature.transfer

import cn.chahuyun.economy.actionchain.annotation.Provider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.feature.user.UserManager
import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain

/**
 * 一次转账指令调用内的「转出方 + 场景」快照。
 *
 * @property userInfo 转出方资料（贷款校验、账户标识用）。
 * @property user 转出方发送者对象（扣款用）。
 * @property subject 回复目标（群或好友）。
 * @property message 触发指令的消息链（解析 @目标 与金额）。
 */
data class TransferActor(
    val userInfo: UserInfoDto,
    val user: User,
    val subject: Contact,
    val message: MessageChain,
)

/**
 * 转账功能的规则链 Provider 集合。
 *
 * 转账没有对外查询需求（`TransferManager` 已因零调用删除），因此本功能不提供 Manager。
 */
object TransferProvider {

    /** [TransferActor] 的 Provider 限定名。 */
    const val ACTOR: String = "transfer-actor"

    /**
     * 加载本次转账指令的参与者快照。
     *
     * @param context 本次调用上下文，从中取出触发事件。
     */
    @Provider(qualifier = ACTOR)
    suspend fun actor(context: ActionContext): TransferActor {
        val event = context.event as? MessageEvent
            ?: error("转账动作只能由消息事件触发，实际事件：${context.event::class.simpleName}")

        val sender = event.sender
        return TransferActor(
            userInfo = UserManager.getUserInfo(sender),
            user = sender,
            subject = event.subject,
            message = event.message,
        )
    }
}
