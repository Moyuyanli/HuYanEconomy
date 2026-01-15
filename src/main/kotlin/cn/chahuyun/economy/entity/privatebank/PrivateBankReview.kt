package cn.chahuyun.economy.entity.privatebank

import jakarta.persistence.*
import java.util.*

/**
 * 私人银行评分（储户评价）
 */
@Entity(name = "PrivateBankReview")
@Table(
    name = "PrivateBankReview",
    indexes = [Index(name = "idx_pb_review_bank", columnList = "bankCode")]
)
class PrivateBankReview(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    var bankCode: String = "",

    var userQq: Long = 0,

    /** 1-5 */
    var rating: Int = 5,

    var createdAt: Date = Date(),
)
