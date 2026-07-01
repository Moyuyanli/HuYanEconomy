package cn.chahuyun.economy.model.fish

import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import kotlinx.serialization.Serializable
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.SingleMessage
import java.util.*

/**
 * 钓鱼排行DTO
 */
@Serializable
data class FishRankingDto(
    /** 记录ID */
    val id: Int = 0,
    /** QQ号 */
    val qq: Long = 0,
    /** 玩家昵称 */
    val name: String = "",
    /** 鱼的尺寸 */
    val dimensions: Int = 0,
    /** 鱼的价格 */
    val money: Double = 0.0,
    /** 鱼竿等级 */
    val fishRodLevel: Int = 0,
    /** 钓到时间 */
    val date: Long = 0,
    /** 鱼种名称 */
    val fishName: String = "",
    /** 鱼塘名称 */
    val fishPondName: String = "",
    /** 鱼等级 */
    val fishLevel: Int = 0,
    /** 鱼塘等级 */
    val fishPondLevel: Int = 0
) {
    fun getInfo(top: Int): SingleMessage {
        var message = "top:${top + 1}\n"
        if (top in 0..2 && date > 0) {
            val s = DateUtil.formatBetween(
                DateUtil.between(Date(), Date(date), DateUnit.MS),
                cn.hutool.core.date.BetweenFormatter.Level.MINUTE
            )
            message += "霸榜时间:$s\n"
        }
        message += "用户:$name(鱼竿等级:$fishRodLevel)\n" +
            "尺寸:$dimensions\n" +
            "金额:$money\n" +
            "鱼:$fishName(等级:$fishLevel)\n" +
            "鱼塘:$fishPondName(鱼塘等级:$fishPondLevel)"
        return PlainText(message)
    }
}
