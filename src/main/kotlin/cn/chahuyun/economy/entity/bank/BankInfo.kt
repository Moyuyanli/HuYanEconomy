package cn.chahuyun.economy.entity.bank

import cn.hutool.core.util.RandomUtil
import jakarta.persistence.*
import java.util.*

/**
 * 银行信息
 *
 * @author Moyuyanli
 * @date 2022/12/22 12:38
 */
@Entity(name = "BankInfo")
@Table(name = "BankInfo")
class BankInfo(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    /**
     * 银行唯一id
     */
    var code: String? = null,

    /**
     * 银行名称
     */
    var name: String? = null,

    /**
     * 银行描述
     */
    var description: String? = null,

    /**
     * 银行管理者qq
     */
    var qq: Long = 0,

    /**
     * 是否每周随机银行利率
     */
    var interestSwitch: Boolean = false,

    /**
     * 注册时间
     */
    var regTime: Date? = null,

    /**
     * 银行注册金额
     */
    var regTotal: Double = 0.0,

    /**
     * 银行总金额
     */
    var total: Double = 0.0,

    /**
     * 银行利率 i%
     */
    var interest: Int = 0
) {

    /**
     * 构造一个银行信息
     *
     * @param code 银行编码
     * @param name 银行名称
     * @param description 银行描述
     * @param qq 银行管理者
     * @param regTotal 注册金额
     */
    constructor(code: String?, name: String?, description: String?, qq: Long, regTotal: Double) : this(
        code = code,
        name = name,
        description = description,
        qq = qq,
        regTime = Date(),
        regTotal = regTotal,
        interestSwitch = true,
        interest = randomInterest()
    )

    companion object {
        /**
         * 随机生成一个利率值
         *
         * 此方法用于模拟生成一个随机的利率值，用于表示用户对某个项目的利率程度
         * 利率值的生成遵循特定的概率分布，以模拟不同用户利率的多样性
         *
         * @return 随机生成的利率值，正数表示较高利率，负数或零表示较低利率
         */
        @JvmStatic
        fun randomInterest(): Int {
            // 随机基数 [1, 100]
            val roll = RandomUtil.randomInt(1, 101)

            // 定义概率阈值
            val highInterestThreshold = 99 // >=99 → 2% 概率
            val mediumInterestThreshold = 35 // >=35 → 64% 概率，<35 → 34%

            return when {
                roll >= highInterestThreshold -> RandomUtil.randomInt(10, 21) // [10, 20]
                roll >= mediumInterestThreshold -> RandomUtil.randomInt(1, 10) // [1, 9]
                else -> RandomUtil.randomInt(-10, 1) // [-10, 0]
            }
        }
    }
}
