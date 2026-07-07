package cn.chahuyun.economy.entity.privatebank

import jakarta.persistence.*
import java.util.*

/**
 * 国卷持仓（归属银行；PrivateBank 模块）
 */
@Entity(name = "PrivateBankGovBondHolding")
@Table(name = "PrivateBankGovBondHolding")
class PrivateBankGovBondHolding(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    var bankCode: String = "",

    var issueId: Int = 0,

    var principal: Double = 0.0,

    var rateMultiplier: Double = 2.0,

    var lockDays: Int = 3,

    var boughtAt: Date = Date(),

    var redeemedAt: Date? = null,
)
