package cn.chahuyun.economy.service

import cn.chahuyun.economy.model.fish.FishRankingDto
import cn.chahuyun.economy.utils.MoneyFormatUtil
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.SingleMessage
import java.util.*

object FishingRankingMessageFormatter {

    fun rankingInfo(ranking: FishRankingDto, top: Int): SingleMessage {
        var message = "top:${top + 1}\n"
        if (top in 0..2 && ranking.date > 0) {
            val duration = DateUtil.formatBetween(
                DateUtil.between(Date(), Date(ranking.date), DateUnit.MS),
                cn.hutool.core.date.BetweenFormatter.Level.MINUTE
            )
            message += "霸榜时间:$duration\n"
        }
        message += "用户:${ranking.name}(鱼竿等级:${ranking.fishRodLevel})\n" +
            "尺寸:${ranking.dimensions}\n" +
            "金额:${MoneyFormatUtil.format(ranking.money)}\n" +
            "鱼:${ranking.fishName}(等级:${ranking.fishLevel})\n" +
            "鱼塘:${ranking.fishPondName}(鱼塘等级:${ranking.fishPondLevel})"
        return PlainText(message)
    }
}
