package cn.chahuyun.economy.utils

import cn.chahuyun.authorize.utils.MessageUtilTemplate
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*

/**
 * 消息工具类
 */
class MessageUtil private constructor() : MessageUtilTemplate(), cn.chahuyun.authorize.utils.MessageUtil {

    override val channel: EventChannel<MessageEvent>
        get() = messageEventChannel

    companion object {
        lateinit var INSTANCE: MessageUtil

        internal lateinit var messageEventChannel: EventChannel<MessageEvent>

        @JvmStatic
        fun init(plugin: JvmPlugin) {
            messageEventChannel = GlobalEventChannel.parentScope(plugin).filterIsInstance<MessageEvent>()
            INSTANCE = MessageUtil()
        }

        @JvmStatic
        fun formatMessage(text: String): PlainText = PlainText(text)

        @JvmStatic
        fun formatMessageChain(text: String): MessageChain = MessageChainBuilder().append(PlainText(text)).build()

        @JvmStatic
        fun formatMessageChain(citation: MessageChain, text: String): MessageChain {
            return MessageChainBuilder().append(QuoteReply(citation)).append(PlainText(text)).build()
        }

        @JvmStatic
        fun formatMessageChain(at: Long, text: String): MessageChain {
            return MessageChainBuilder().append(At(at)).append(PlainText(text)).build()
        }

        @JvmStatic
        fun quoteReply(citation: MessageChain): MessageChainBuilder {
            return MessageChainBuilder().append(QuoteReply(citation))
        }

        @JvmStatic
        fun formatMessageBuild(text: String): MessageChainBuilder = MessageChainBuilder().append(PlainText(text))

        @JvmStatic
        fun formatMessageBuild(citation: MessageChain, text: String): MessageChainBuilder {
            return MessageChainBuilder().append(QuoteReply(citation)).append(PlainText(text))
        }

        @JvmStatic
        fun formatMessageBuild(at: Long, text: String): MessageChainBuilder {
            return MessageChainBuilder().append(At(at)).append(PlainText(text))
        }
    }
}
