package cn.chahuyun.economy.feature.bond

import cn.chahuyun.economy.actionchain.annotation.Provider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain

/**
 * 一次国卷指令调用内的「操作者 + 场景」快照。
 *
 * @property sender 操作者（行长身份由 `PrivateBankManager.ownedBank` 判定）。
 * @property subject 回复目标（群或好友）。
 * @property message 触发指令的消息链（引用回复用）。
 * @property content 指令纯文本（参数解析用）。
 */
data class BondActor(
    val sender: User,
    val subject: Contact,
    val message: MessageChain,
    val content: String,
)

/**
 * 国卷功能的规则链 Provider 集合。
 */
object BondProvider {

    /** [BondActor] 的 Provider 限定名。 */
    const val ACTOR: String = "bond-actor"

    /**
     * 加载本次国卷指令的参与者快照。
     *
     * @param context 本次调用上下文，从中取出触发事件。
     */
    @Provider(qualifier = ACTOR)
    suspend fun actor(context: ActionContext): BondActor {
        val event = context.event as? MessageEvent
            ?: error("国卷动作只能由消息事件触发，实际事件：${context.event::class.simpleName}")

        return BondActor(
            sender = event.sender,
            subject = event.subject,
            message = event.message,
            content = event.message.contentToString(),
        )
    }
}
