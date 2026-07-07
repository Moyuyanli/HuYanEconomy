package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.constant.FishPondLevelConstant
import cn.chahuyun.economy.model.fish.FishBait
import cn.chahuyun.economy.model.fish.FishDto
import cn.chahuyun.economy.model.fish.FishPondDto
import cn.chahuyun.economy.utils.FormatUtil
import cn.chahuyun.economy.utils.MoneyFormatUtil
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.MessageChainBuilder

internal object FishingMessageBuilder {

    fun startFishing(userName: String, fishBait: FishBait, fishPond: FishPondDto): String =
        " ${userName}开始钓鱼\n" +
            "鱼饵:${fishBait.name}\n" +
            "鱼塘:${fishPond.name}\n" +
            "等级:${fishPond.pondLevel}\n" +
            "最低鱼竿等级:${fishPond.minLevel}\n" +
            fishPond.description

    fun success(userId: Long, fish: FishDto, dimensions: Int, money: Double): Message {
        val result =
            "\n起竿咯！\n${fish.name}\n等级:${fish.level}\n单价:${fish.price}\n尺寸:$dimensions\n总金额:$money\n${fish.description}"
        return MessageChainBuilder().append(At(userId)).append(result).build()
    }

    fun pondInfo(fishPond: FishPondDto, money: Double): String {
        val level = fishPond.pondLevel

        if (level >= FishPondLevelConstant.MAX_LEVEL) {
            return "当前鱼塘信息:\n" +
                "鱼塘名称:${fishPond.name}\n" +
                "鱼塘等级:${level} (已满级)\n" +
                "鱼塘钓鱼次数:${fishPond.number}\n" +
                "鱼塘最低鱼竿等级:${fishPond.minLevel}\n" +
                "鱼塘金额:${MoneyFormatUtil.format(money)}"
        }

        val value = FishPondLevelConstant.entries[level - 1]
        return "当前鱼塘信息:\n" +
            "鱼塘名称:${fishPond.name}\n" +
            "鱼塘等级:${level}\n" +
            "鱼塘钓鱼次数:${fishPond.number}\n" +
            "鱼塘最低鱼竿等级:${fishPond.minLevel}\n" +
            "鱼塘升级所需金额:${MoneyFormatUtil.format(value.amount.toDouble())}\n" +
            "鱼塘金额:${MoneyFormatUtil.format(money)}\n" +
            "鱼塘升级进度:${FormatUtil.fixed(money / value.amount * 100, 1)}%"
    }
}
