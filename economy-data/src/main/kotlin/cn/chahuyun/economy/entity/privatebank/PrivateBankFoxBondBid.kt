package cn.chahuyun.economy.entity.privatebank

import jakarta.persistence.*
import java.util.*

/**
 * 狐卷竞标记录
 */
@Entity(name = "PrivateBankFoxBondBid")
@Table(
    name = "PrivateBankFoxBondBid",
    indexes = [
        Index(name = "idx_pb_foxbond_bid_code", columnList = "bondCode"),
        Index(name = "idx_pb_foxbond_bid_bank", columnList = "bondCode,bankCode", unique = true)
    ]
)
class PrivateBankFoxBondBid(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    var bondCode: String = "",

    var bankCode: String = "",

    var ownerQq: Long = 0,

    /** 溢价金额（中标即扣除销毁） */
    var premium: Double = 0.0,

    /** 接受利息（日利，百分比），必须 <= baseRate */
    var bidRate: Double = 0.0,

    var createdAt: Date = Date(),
)
