package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.usecase.BackpackUsecase
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 背包管理
 *
 * @author Moyuyanli
 * @date 2022/11/15 10:00
 */
@EventComponent
class BackpackAction {

    @MessageAuthorize(text = ["我的背包", "backpack"])
    suspend fun viewBackpack(event: GroupMessageEvent) {
        BackpackUsecase.viewBackpack(event)
    }

    @MessageAuthorize(
        text = ["use( \\d+)+|使用( \\d+)+"],
        messageMatching = MessageMatchingEnum.REGULAR
    )
    suspend fun useProp(event: GroupMessageEvent) {
        BackpackUsecase.useProp(event)
    }

    @MessageAuthorize(
        text = ["dis( \\d+)+|丢弃( \\d+)+"],
        messageMatching = MessageMatchingEnum.REGULAR
    )
    suspend fun discard(event: GroupMessageEvent) {
        BackpackUsecase.discard(event)
    }
}
