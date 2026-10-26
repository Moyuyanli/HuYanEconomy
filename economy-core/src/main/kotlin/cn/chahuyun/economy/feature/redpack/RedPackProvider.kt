package cn.chahuyun.economy.feature.redpack

import cn.chahuyun.economy.actionchain.annotation.Provider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain

/**
 * 一次红包指令调用内的「领取者 + 场景」快照。
 *
 * @property sender 指令发送者（扣款、领取判定用）。
 * @property subject 回复目标（通常是群）。
 * @property group 所在群；非群消息时为 `null`（只有「全局红包列表」允许）。
 * @property message 触发指令的消息链（引用回复用）。
 * @property content 指令纯文本（口令与参数解析用）。
 */
data class RedPackActor(
    val sender: User,
    val subject: Contact,
    val group: Group?,
    val message: MessageChain,
    val content: String,
)

/**
 * 红包功能的规则链 Provider 集合。
 */
object RedPackProvider {

    /** [RedPackActor] 的 Provider 限定名。 */
    const val ACTOR: String = "redpack-actor"

    /**
     * 加载本次红包指令的参与者快照。
     *
     * @param context 本次调用上下文，从中取出触发事件。
     */
    @Provider(qualifier = ACTOR)
    suspend fun actor(context: ActionContext): RedPackActor {
        val event = context.event as? MessageEvent
            ?: error("红包动作只能由消息事件触发，实际事件：${context.event::class.simpleName}")

        return RedPackActor(
            sender = event.sender,
            subject = event.subject,
            group = (event as? GroupMessageEvent)?.group,
            message = event.message,
            content = event.message.contentToString(),
        )
    }
}
