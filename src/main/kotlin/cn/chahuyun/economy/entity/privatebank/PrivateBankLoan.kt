package cn.chahuyun.economy.entity.privatebank

import jakarta.persistence.*
import java.util.*

/**
 * 实际借款单
 */
@Entity(name = "PrivateBankLoan")
@Table(name = "PrivateBankLoan")
class PrivateBankLoan(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    var offerId: Int = 0,

    var bankCode: String = "",

    var lenderQq: Long = 0,

    var borrowerQq: Long = 0,

    var principal: Double = 0.0,

    var interest: Int = 10,

    var termDays: Int = 7,

    var createdAt: Date = Date(),

    var dueAt: Date = Date(),

    var repaidAt: Date? = null,
)
