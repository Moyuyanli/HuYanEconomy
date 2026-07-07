package cn.chahuyun.economy.usecase

import cn.chahuyun.authorize.utils.AuthMessageUtil.sendMessageQuote
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.MessageChain

internal object GameUsecaseReplySupport {

    suspend fun reply(event: MessageEvent, message: String) {
        event.subject.sendMessage(format(event.message, message))
    }

    suspend fun reply(event: GroupMessageEvent, message: String) {
        reply(event.subject, event.message, message)
    }

    suspend fun quote(event: GroupMessageEvent, message: String) {
        event.sendMessageQuote(message)
    }

    suspend fun send(event: GroupMessageEvent, message: Message) {
        event.subject.sendMessage(message)
    }

    suspend fun plain(event: MessageEvent, message: String) {
        event.subject.sendMessage(message)
    }

    suspend fun plain(group: Group, message: String) {
        group.sendMessage(message)
    }

    suspend fun reply(group: Group, source: MessageChain, message: String) {
        group.sendMessage(format(source, message))
    }

    suspend fun reply(group: Group, userId: Long, message: String) {
        group.sendMessage(MessageUtil.formatMessageChain(userId, message))
    }

    fun format(source: MessageChain, message: String): MessageChain =
        MessageUtil.formatMessageChain(source, message)
}
