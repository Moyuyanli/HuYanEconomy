package cn.chahuyun.economy.sign

import cn.chahuyun.economy.entity.UserInfo
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply

/**
 * 签到事件管理
 */
class SignEvent(
    val userInfo: UserInfo,
    val event: GroupMessageEvent
) : AbstractEvent() {
    val group: Group = event.group
    val messages: MessageChain = event.message

    var param: Int? = null
    var reply: MessageChain? = null
    var eventReply: MessageChainBuilder? = null
    var sign_2: Boolean = false
    var sign_3: Boolean = false
    var sign_in: Boolean = false
    var gold: Double? = null

    /**
     * 添加事件消息回复
     */
    fun eventReplyAdd(messages: MessageChain) {
        if (eventReply == null) {
            eventReply = MessageChainBuilder()
            eventReply?.add(messages)
        } else {
            eventReply?.add(PlainText("\n"))
            eventReply?.add(messages)
        }
    }

    /**
     * 引用回复
     */
    suspend fun sendQuote(messages: MessageChain): MessageReceipt<Group> {
        return group.sendMessage(QuoteReply(this.messages).plus(messages))
    }
}
