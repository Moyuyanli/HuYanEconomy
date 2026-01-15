package cn.chahuyun.economy.event

import cn.chahuyun.economy.utils.Log
import net.mamoe.mirai.contact.BotIsBeingMutedException
import net.mamoe.mirai.contact.MessageTooLargeException
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.EventCancelledException
import kotlin.coroutines.CoroutineContext

/**
 * 消息检测
 */
@Deprecated("1.6.3")
class MessageEventListener : SimpleListenerHost() {

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        when (exception) {
            is EventCancelledException -> Log.error("发送消息被取消:", exception)
            is BotIsBeingMutedException -> Log.error("你的机器人被禁言:", exception)
            is MessageTooLargeException -> Log.error("发送消息过长:", exception)
            is IllegalArgumentException -> Log.error("发送消息为空:", exception)
        }
        Log.error(exception.cause ?: exception)
    }
}
