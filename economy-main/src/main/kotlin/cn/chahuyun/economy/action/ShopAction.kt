package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.feature.shop.ShopUsecaseActions
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 道具商店事件
 *
 * 只做 Mirai 指令注册，页码与购买清单解析在 `feature/shop` 的规则链里。
 *
 * @author Moyuyanli
 * @date 2024/9/25 10:40
 */
@EventComponent
class ShopAction {

    @MessageAuthorize(text = ["道具商店( \\d+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun viewShop(event: GroupMessageEvent) {
        ShopUsecaseActions.viewShop(event)
    }

    @MessageAuthorize(
        text = ["buy( \\S+)+|购买道具( \\S+)+"],
        messageMatching = MessageMatchingEnum.REGULAR
    )
    suspend fun buyProp(event: GroupMessageEvent) {
        ShopUsecaseActions.buyProp(event)
    }
}
