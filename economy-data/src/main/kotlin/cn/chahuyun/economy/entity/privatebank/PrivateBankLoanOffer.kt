package cn.chahuyun.economy.entity.privatebank

import jakarta.persistence.*
import java.util.*

/**
 * 贷款标的（行长发布）
 */
@Entity(name = "PrivateBankLoanOffer")
@Table(name = "PrivateBankLoanOffer")
class PrivateBankLoanOffer(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    var bankCode: String = "",

    var ownerQq: Long = 0,

    /** 资金来源：LIQUIDITY 或 OWNER */
    var source: String = "LIQUIDITY",

    var total: Double = 0.0,

    var remaining: Double = 0.0,

    /** 利率（与主银行同量纲，日利率 = interest/1000） */
    var interest: Int = 10,

    var termDays: Int = 7,

    var enabled: Boolean = true,

    var createdAt: Date = Date(),
)
