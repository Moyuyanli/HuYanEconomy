package cn.chahuyun.economy.entity.privatebank

import jakarta.persistence.*
import java.util.*

@Entity(name = "PrivateBankMainBankDebt")
@Table(name = "PrivateBankMainBankDebt")
class PrivateBankMainBankDebt(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    @Column(unique = true)
    var bankCode: String = "",

    var principal: Double = 0.0,

    var accruedInterest: Double = 0.0,

    var lastAccruedAt: Date = Date(),

    var createdAt: Date = Date(),

    var updatedAt: Date = Date(),

    var repaidAt: Date? = null,
)
