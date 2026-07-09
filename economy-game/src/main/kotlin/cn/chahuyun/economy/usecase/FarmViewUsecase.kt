package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.image.FarmDetailImageRenderer
import cn.chahuyun.economy.service.FarmViewService
import cn.chahuyun.economy.utils.ImageMessageUtil
import cn.chahuyun.economy.utils.Log
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

    suspend fun viewFarmDetail(event: GroupMessageEvent) {
        try {
            val card = FarmViewService.farmDetailCard(
                qq = event.sender.id,
                owner = event.sender.id.toString(),
            )
            ImageMessageUtil.sendQuotedImage(event.subject, event.message, FarmDetailImageRenderer.render(card))
        } catch (e: Exception) {
            Log.error("农场详情图片生成或发送失败", e)
            FarmUsecaseSupport.reply(event, "农场详情图片生成失败，请稍后再试。")
        }
    }

    suspend fun blackMarket(event: GroupMessageEvent) {
        FarmUsecaseSupport.replyView(event) { FarmViewService.blackMarketText(it) }
    }
}
