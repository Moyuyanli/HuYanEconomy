package cn.chahuyun.economy.service

import cn.chahuyun.economy.constant.FishPondLevelConstant
import cn.chahuyun.economy.entity.fishing.FishEntity
import cn.chahuyun.economy.entity.fishing.UserPondEntity
import cn.chahuyun.economy.model.fish.FishingBait
import cn.chahuyun.economy.utils.MoneyFormatUtil
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.MessageChainBuilder
import java.math.BigDecimal

/**
 * 钓鱼玩法文案构建。
 */
internal object FishingMessageBuilder {

    fun startFishing(userName: String, bait: FishingBait, pond: UserPondEntity): String =
        " ${userName}开始钓鱼\n" +
            "鱼饵:${bait.name}\n" +
            "鱼塘:${pond.name}\n" +
            "鱼塘等级:${pond.level}\n" +
            "收费率:${feeText(pond)}"

    fun success(userId: Long, fish: FishEntity, dimensions: Int, money: BigDecimal): Message {
        val result = "\n起竿咯！\n${fish.name}\n等级:${fish.level}\n单价:${MoneyFormatUtil.format(fish.price.toDouble())}\n" +
            "尺寸:$dimensions\n总金额:${MoneyFormatUtil.format(money.toDouble())}\n${fish.description}"
        return MessageChainBuilder().append(At(userId)).append(result).build()
    }

    fun pondInfo(pond: UserPondEntity, money: BigDecimal): String {
        val level = pond.level
        val base = buildString {
            append("当前鱼塘信息:\n")
            append("鱼塘名称:${pond.name}\n")
            append("鱼塘等级:$level").append(if (level >= FishPondLevelConstant.MAX_LEVEL) " (已满级)\n" else "\n")
            append("鱼塘钓鱼次数:${pond.totalFishingCount}\n")
            append("鱼塘收费率:${feeText(pond)}\n")
            append("可钓鱼等级上限:${FishingPondService.maxFishLevel(pond)}\n")
            append("鱼塘金额:${MoneyFormatUtil.format(money.toDouble())}")
        }
        if (level >= FishPondLevelConstant.MAX_LEVEL) return base

        val next = FishPondLevelConstant.of(level + 1) ?: return base
        val progress = if (next.amount <= 0) 100.0 else (money.toDouble() / next.amount * 100).coerceIn(0.0, 100.0)
        return base + "\n鱼塘升级所需金额:${MoneyFormatUtil.format(next.amount.toDouble())}\n" +
            "鱼塘升级所需鱼竿等级:${next.minRodLevel}\n" +
            "鱼塘升级进度:${"%.1f".format(progress)}%"
    }

    fun feeText(pond: UserPondEntity): String {
        val permille = pond.feeRatePermille.coerceIn(0, 1000)
        return if (permille % 10 == 0) "${permille / 10}%" else "${permille / 10.0}%"
    }
}
