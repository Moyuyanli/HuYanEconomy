package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.model.farm.FarmViewState
import cn.chahuyun.economy.service.FarmCommandTextParser
import cn.chahuyun.economy.service.FarmViewService
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChain

internal object FarmUsecaseSupport {

    suspend fun replyView(event: GroupMessageEvent, render: (FarmViewState) -> String) {
        val state = FarmViewService.getOrCreateViewState(event.sender.id)
        reply(event, render(state))
    }

    suspend fun reply(event: GroupMessageEvent, message: String) {
        event.subject.sendMessage(formatReply(event.message, message))
    }

    fun commandPayload(event: GroupMessageEvent, command: String): String =
        FarmCommandTextParser.payload(event.message.contentToString(), command)

    fun commandPayload(event: GroupMessageEvent, commands: List<String>): String =
        FarmCommandTextParser.payload(event.message.contentToString(), commands)

    suspend fun atTargetOrReply(event: GroupMessageEvent): Long? {
        val at = event.message.filterIsInstance<At>().firstOrNull()
        if (at == null) {
            reply(event, "格式: 帮浇水 @用户")
            return null
        }
        return at.target
    }

    private fun formatReply(source: MessageChain, message: String): MessageChain =
        MessageUtil.formatMessageChain(source, message)
}
