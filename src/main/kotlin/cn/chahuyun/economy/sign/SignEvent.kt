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
 *
 * @author Moyuyanli
 * @date 2024/9/25 16:58
 */
class SignEvent(
    val userInfo: UserInfo,
    val event: GroupMessageEvent
) : AbstractEvent() {

    val group: Group = event.group
    val messages: MessageChain = event.message

    /**
     * 随机参数
     */
    var param: Int? = null

    /**
     * 文字消息
     */
    var reply: MessageChain? = null

    /**
     * 事件回复消息
     */
    var eventReply: MessageChainBuilder? = null

    /**
     * 双倍签到
     */
    var sign_2: Boolean = false

    /**
     * 三倍签到
     */
    var sign_3: Boolean = false

    /**
     * 补签
     */
    var sign_in: Boolean = false

    /**
     * 签到金额
     */
    var gold: Double? = null

    /**
     * 添加事件消息回复
     *
     * @param messages 事件消息
     */
    fun eventReplyAdd(messages: MessageChain) {
        if (this.eventReply == null) {
            this.eventReply = MessageChainBuilder()
            this.eventReply!!.add(messages)
        } else {
            this.eventReply!!.add(PlainText("\n"))
            this.eventReply!!.add(messages)
        }
    }

    /**
     * 引用回复
     *
     * @param messages 回复的消息
     * @return 返回的消息
     */
    suspend fun sendQuote(messages: MessageChain): MessageReceipt<Group> {
        return group.sendMessage(QuoteReply(this.messages).plus(messages))
    }
}

