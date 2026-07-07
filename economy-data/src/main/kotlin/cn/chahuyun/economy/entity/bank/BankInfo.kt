package cn.chahuyun.economy.entity.bank

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
        interestSwitch = true
    )
}
