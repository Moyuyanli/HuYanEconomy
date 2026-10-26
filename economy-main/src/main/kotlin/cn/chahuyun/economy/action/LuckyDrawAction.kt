@file:Suppress("DuplicatedCode")

package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.games.raffle.RaffleUsecaseActions
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 抽奖指令入口：单抽 / 十连。
 */
@EventComponent
class LuckyDrawAction {

    @MessageAuthorize(
        ["raffle", "抽奖"], groupPermissions = [EconPerm.RAFFLE_PERM]
    )
    suspend fun luckyDraw(event: GroupMessageEvent) {
        RaffleUsecaseActions.luckyDraw(event)
    }

    @MessageAuthorize(
        ["ten", "十连"], groupPermissions = [EconPerm.RAFFLE_PERM]
    )
    suspend fun luckyDrawTen(event: GroupMessageEvent) {
        RaffleUsecaseActions.luckyDrawTen(event)
    }
}
