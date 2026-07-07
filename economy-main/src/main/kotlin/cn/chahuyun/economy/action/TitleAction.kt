package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.usecase.TitleUsecase
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 称号管理
 */
@EventComponent
class TitleAction {

    /**
     * 查询拥有的称号
     */
    @MessageAuthorize(text = ["我的称号", "称号列表", "拥有称号"])
    suspend fun viewTitleInfo(event: MessageEvent) {
        TitleUsecase.viewTitleInfo(event)
    }

    /**
     * 查询拥有的称号
     */
    @MessageAuthorize(text = ["称号商店"])
    suspend fun viewCanByTitle(event: MessageEvent) {
        TitleUsecase.viewCanByTitle(event)
    }

    /**
     * 购买称号
     */
    @MessageAuthorize(text = ["购买称号 (\\S+)"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun buyTitle(event: MessageEvent) {
        TitleUsecase.buyTitle(event)
    }

    /**
     * 切换称号
     */
    @MessageAuthorize(text = ["切换称号 (\\d+)"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun userTitle(event: MessageEvent) {
        TitleUsecase.userTitle(event)
    }
}
