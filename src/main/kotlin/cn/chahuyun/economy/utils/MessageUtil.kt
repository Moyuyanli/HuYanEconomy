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
        fun formatMessage(format: String, vararg params: Any?): PlainText {
            return PlainText(String.format(format, *params))
        }

        @JvmStatic
        fun formatMessageChain(format: String, vararg params: Any?): MessageChain {
            return MessageChainBuilder().append(String.format(format, *params)).build()
        }

        @JvmStatic
        fun formatMessageChain(citation: MessageChain, format: String, vararg params: Any?): MessageChain {
            return MessageChainBuilder().append(QuoteReply(citation)).append(String.format(format, *params)).build()
        }

        @JvmStatic
        fun formatMessageChain(at: Long, format: String, vararg params: Any?): MessageChain {
            return MessageChainBuilder().append(At(at)).append(String.format(format, *params)).build()
        }

        @JvmStatic
        fun quoteReply(citation: MessageChain): MessageChainBuilder {
            return MessageChainBuilder().append(QuoteReply(citation))
        }

        @JvmStatic
        fun formatMessageBuild(format: String, vararg params: Any?): MessageChainBuilder {
            return MessageChainBuilder().append(String.format(format, *params))
        }

        @JvmStatic
        fun formatMessageBuild(citation: MessageChain, format: String, vararg params: Any?): MessageChainBuilder {
            return MessageChainBuilder().append(QuoteReply(citation)).append(String.format(format, *params))
        }

        @JvmStatic
        fun formatMessageBuild(at: Long, format: String, vararg params: Any?): MessageChainBuilder {
            return MessageChainBuilder().append(At(at)).append(String.format(format, *params))
        }
    }
}
