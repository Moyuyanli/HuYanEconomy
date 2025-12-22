package cn.chahuyun.economy.entity

import cn.chahuyun.hibernateplus.HibernateFactory
import jakarta.persistence.*
import java.io.Serializable

/**
 * 彩票信息
 *
 * @author Moyuyanli
 * @date 2022/12/6 8:55
 */
@Entity(name = "LotteryInfo")
@Table
class LotteryInfo(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    /**
     * 购买用户
     */
    var qq: Long = 0,

    /**
     * 购买群号
     */
    @Column(name = "group_number")
    var group: Long = 0,

    /**
     * 购买金额
     */
    var money: Double = 0.0,

    /**
     * 购买类型
     * 1:分钟彩票
     * 2:小时彩票
     * 3:天彩票
     */
    var type: Int = 0,

    /**
     * 购买号码
     */
    var number: String? = null,

    /**
     * 本期号码
     */
    var current: String? = null,

    /**
     * 获得奖金
     */
    var bonus: Double = 0.0
) : Serializable {

    /**
     * 删除
     */
    fun remove() {
        HibernateFactory.delete(this)
    }

    /**
     * 转换为消息
     */
    fun toMessage(): String {
        val stringBuilder = StringBuilder()
        when (type) {
            1 -> stringBuilder.append("你的小签已经开签了！")
            2 -> stringBuilder.append("你的中签已经开签了！")
            else -> stringBuilder.append("你的大签已经开签了！")
        }
        stringBuilder.append("\n本期号码:$current")
            .append("\n你的号码:$number")
            .append("\n获得奖金为:$bonus")
        return stringBuilder.toString()
    }
}
