package cn.chahuyun.economy.service

import cn.chahuyun.economy.data.repository.FishRepository
import cn.chahuyun.economy.model.fish.getInfo
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText

object FishingRankingService {

    suspend fun fishTop(event: MessageEvent) {
        Log.info("钓鱼榜指令")
        val bot = event.bot
        val subject = event.subject
        val rankingList = FishRepository.topRankingByMoney(limit = 10)

        if (rankingList.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "暂时没人钓鱼!"))
            return
        }

        val forwardMessage = ForwardMessageBuilder(subject).apply {
            add(bot, PlainText("钓鱼排行榜:"))
            rankingList.forEachIndexed { index, ranking ->
                add(bot, ranking.getInfo(index))
            }
        }
        subject.sendMessage(forwardMessage.build())
    }
}
