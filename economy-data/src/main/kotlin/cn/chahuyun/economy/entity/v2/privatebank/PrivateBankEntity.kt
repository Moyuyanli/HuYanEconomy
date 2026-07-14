package cn.chahuyun.economy.entity.v2.privatebank

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "PrivateBankEntityV2")
@Table(
    name = "hye_private_bank",
    indexes = [
        Index(name = "idx_hye_private_bank_code", columnList = "code", unique = true),
        Index(name = "idx_hye_private_bank_owner", columnList = "owner_qq")
    ]
)
class PrivateBankEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "code", length = 128)
    var code: String = "",

    @Column(name = "name", length = 128)
    var name: String = "",

    @Column(name = "slogan", length = 512)
    var slogan: String = "",

    @Column(name = "owner_qq", nullable = false)
    var ownerQq: Long = 0,

    @Column(name = "vip_only", nullable = false)
    var vipOnly: Boolean = false,

    @Lob
    @Column(name = "vip_whitelist", columnDefinition = "TEXT")
    var vipWhitelist: String = "",

    @Column(name = "depositor_interest", nullable = false)
    var depositorInterest: Int = 5,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0,

    @Column(name = "defaulter_until", nullable = false)
    var defaulterUntil: Long = 0,

    @Column(name = "withdraw_requests", nullable = false)
    var withdrawRequests: Int = 0,

    @Column(name = "withdraw_failures", nullable = false)
    var withdrawFailures: Int = 0,

    @Column(name = "star", nullable = false)
    var star: Int = 1,

    @Column(name = "avg_review", nullable = false)
    var avgReview: Double = 0.0,

    @Column(name = "bankrupt_at")
    var bankruptAt: Long? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0
) : Serializable
