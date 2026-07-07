package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.economy.service.HelpImageService
import net.mamoe.mirai.event.events.GroupMessageEvent

@EventComponent
class HelpAction {

    @MessageAuthorize(["help", "帮助"])
    suspend fun help(event: GroupMessageEvent) {
        HelpImageService.sendMainHelp(event)
    }

    @MessageAuthorize(["gameHelp", "游戏帮助"])
    suspend fun gameHelp(event: GroupMessageEvent) {
        HelpImageService.sendGameHelp(event)
    }
}
