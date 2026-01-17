package cn.chahuyun.economy.entity.privatebank

import jakarta.persistence.*
import java.util.*

/**
 * 狐卷持仓（归属银行；PrivateBank 模块）
 */
@Entity(name = "PrivateBankFoxBondHolding")
@Table(
    name = "PrivateBankFoxBondHolding",
    indexes = [
        Index(name = "idx_pb_foxbond_holding_bank", columnList = "bankCode"),
        Index(name = "idx_pb_foxbond_holding_redeemed", columnList = "redeemedAt")
    ]
)
class PrivateBankFoxBondHolding(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    var bondCode: String = "",

    var bankCode: String = "",

    /** 锁定面额 */
    var principal: Double = 0.0,

    /** 实际日利（百分比） */
    var rate: Double = 0.0,

    var startedAt: Date = Date(),

    var dueAt: Date = Date(),

    var redeemedAt: Date? = null,
)
