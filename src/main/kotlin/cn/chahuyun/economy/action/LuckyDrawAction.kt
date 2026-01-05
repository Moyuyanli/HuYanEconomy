@file:Suppress("DuplicatedCode")

package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.usecase.LuckyDrawUsecase
import net.mamoe.mirai.event.events.GroupMessageEvent

@EventComponent
class LuckyDrawAction {

    @MessageAuthorize(
        ["raffle", "抽奖"], groupPermissions = [EconPerm.RAFFLE_PERM]
    )
    suspend fun luckyDraw(event: GroupMessageEvent) {
        LuckyDrawUsecase.luckyDraw(event)
    }

    @MessageAuthorize(
        ["ten", "十连"], groupPermissions = [EconPerm.RAFFLE_PERM]
    )
    suspend fun luckyDrawTen(event: GroupMessageEvent) {
        LuckyDrawUsecase.luckyDrawTen(event)
    }
}