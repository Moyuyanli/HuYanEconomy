package cn.chahuyun.economy.entity.privatebank

import jakarta.persistence.*
import java.util.*

/**
 * 储户在某银行的存款记录（PrivateBank 模块；本金+利息累积）。
 */
@Entity(name = "PrivateBankDeposit")
@Table(
    name = "PrivateBankDeposit",
    indexes = [Index(name = "idx_pb_deposit_bank_user", columnList = "bankCode,userQq", unique = true)]
)
class PrivateBankDeposit(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    var bankCode: String = "",

    var userQq: Long = 0,

    /** 当前本金（含利息累积） */
    var principal: Double = 0.0,

    var createdAt: Date = Date(),

    var updatedAt: Date = Date(),
)
