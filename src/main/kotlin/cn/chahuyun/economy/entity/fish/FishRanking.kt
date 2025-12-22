package cn.chahuyun.economy.entity.fish

import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import jakarta.persistence.*
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.SingleMessage
import java.io.Serializable
import java.util.*

/**
 * 钓鱼排行
 *
 * @author Moyuyanli
 * @date 2022/12/14 15:08
 */
@Entity(name = "FishRanking")
@Table
class FishRanking(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    /**
     * 钓起着qq
     */
    var qq: Long = 0,

    /**
     * 名称
     */
    var name: String? = null,

    /**
     * 尺寸
     */
    var dimensions: Int = 0,

    /**
     * 金额
     */
    var money: Double = 0.0,

    /**
     * 鱼竿等级
     */
    var fishRodLevel: Int = 0,

    /**
     * 钓起来的时间
     */
    var date: Date? = null,

    /**
     * 钓起来的鱼
     */
    @ManyToOne(targetEntity = Fish::class, fetch = FetchType.EAGER)
    @JoinColumn(name = "FishId")
    var fish: Fish? = null,

    /**
     * 钓起来的鱼塘
     */
    @ManyToOne(targetEntity = FishPond::class, fetch = FetchType.EAGER)
    @JoinColumn(name = "FishPondId")
    var fishPond: FishPond? = null
) : Serializable {

    constructor(qq: Long, name: String?, dimensions: Int, money: Double, fishRodLevel: Int, fish: Fish?, fishPond: FishPond?) : this(
        qq = qq,
        name = name,
        dimensions = dimensions,
        money = money,
        fishRodLevel = fishRodLevel,
        fish = fish,
        fishPond = fishPond,
        date = Date()
    )

    /**
     * 显示排行榜信息
     *
     * @param top 名次
     * @return 消息
     */
    fun getInfo(top: Int): SingleMessage {
        var message = "top:${top + 1}\n"
        val rankingDate = date
        if (top in 0..2 && rankingDate != null) {
            val s = DateUtil.formatBetween(DateUtil.between(Date(), rankingDate, DateUnit.MS), cn.hutool.core.date.BetweenFormatter.Level.MINUTE)
            message += "霸榜时间:$s\n"
        }
        message += "用户:$name(鱼竿等级:$fishRodLevel)\n" +
                "尺寸:$dimensions\n" +
                "金额:$money\n" +
                "鱼:${fish?.name}(等级:${fish?.level})\n" +
                "鱼塘:${fishPond?.name}(鱼塘等级:${fishPond?.pondLevel})"
        return PlainText(message)
    }
}
