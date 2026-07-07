package cn.chahuyun.economy.entity

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

    constructor(userId: Long, group: Long, money: Double, type: Int, number: String) : this(
        0,
        userId,
        group,
        money,
        type,
        number,
    )
}
