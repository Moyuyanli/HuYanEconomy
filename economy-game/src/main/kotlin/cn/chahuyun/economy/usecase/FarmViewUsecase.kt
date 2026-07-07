package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.service.FarmViewService
import net.mamoe.mirai.event.events.GroupMessageEvent

object FarmViewUsecase {

    suspend fun viewFarm(event: GroupMessageEvent) {
        FarmUsecaseSupport.replyView(event) { FarmViewService.renderFarm(it) }
    }

    suspend fun viewShop(event: GroupMessageEvent) {
        FarmUsecaseSupport.replyView(event) { FarmViewService.renderShop(it) }
    }

    suspend fun viewWarehouse(event: GroupMessageEvent) {
        FarmUsecaseSupport.replyView(event) { FarmViewService.renderWarehouse(it) }
    }

    suspend fun blackMarket(event: GroupMessageEvent) {
        FarmUsecaseSupport.replyView(event) { FarmViewService.blackMarketText(it) }
    }
}
