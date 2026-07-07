package cn.chahuyun.economy.service

import cn.chahuyun.economy.model.LotteryInfoDto
import cn.chahuyun.economy.utils.MoneyFormatUtil
import net.mamoe.mirai.contact.NormalMember

object LotteryMessageFormatter {

    fun result(lotteryInfo: LotteryInfoDto): String {
        val title = when (lotteryInfo.type) {
            1 -> "你的小签已经开签了！"
            2 -> "你的中签已经开签了！"
            else -> "你的大签已经开签了！"
        }
        return StringBuilder(title)
            .append("\n本期号码:${lotteryInfo.current}")
            .append("\n你的号码:${lotteryInfo.number}")
            .append("\n获得奖金为:${lotteryInfo.bonus}")
            .toString()
    }

    fun groupWinner(member: NormalMember, bonus: Double): String =
        "中奖者:${member.nick}(${member.id}),奖金${MoneyFormatUtil.format(bonus)}金币"
}
