package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.usecase.FundingUsecase
import net.mamoe.mirai.event.events.FriendMessageEvent

@EventComponent
class FundingAction {

    @MessageAuthorize(text = ["#fund bind \\d+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun fundBind(event: FriendMessageEvent) {
        FundingUsecase.fundBind(event)
    }

    @MessageAuthorize(text = ["#fund get \\S+ \\d+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun fundGet(event: FriendMessageEvent) {
        FundingUsecase.fundGet(event)
    }
}
