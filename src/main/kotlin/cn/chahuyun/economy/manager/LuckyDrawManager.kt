package cn.chahuyun.economy.manager

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.economy.constant.EconPerm
import net.mamoe.mirai.event.events.GroupMessageEvent

@EventComponent
class LuckyDrawManager {


    @MessageAuthorize(["raffle","抽奖"],
        groupPermissions = [EconPerm.RAFFLE_PERM])
    suspend fun luckyDraw(event: GroupMessageEvent) {
        event.subject.sendMessage("还没写完哩！")
    }

}