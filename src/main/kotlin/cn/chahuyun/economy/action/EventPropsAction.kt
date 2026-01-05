package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.usecase.EventPropsUsecase
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 道具商店事件
 *
 * @author Moyuyanli
 * @date 2024/9/25 10:40
 */
@EventComponent
class EventPropsAction {

    @MessageAuthorize(text = ["道具商店( \\d+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun viewShop(event: GroupMessageEvent) {
        EventPropsUsecase.viewShop(event)
    }

    @MessageAuthorize(
        text = ["buy( \\S+)+|购买道具( \\S+)+"],
        messageMatching = MessageMatchingEnum.REGULAR
    )
    suspend fun buyProp(event: GroupMessageEvent) {
        EventPropsUsecase.buyProp(event)
    }
}
