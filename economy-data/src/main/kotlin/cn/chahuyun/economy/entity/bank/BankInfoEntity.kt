package cn.chahuyun.economy.entity.bank

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "BankInfoEntity")
@Table(name = "hye_bank_info", indexes = [
    Index(name = "uk_hye_bank_info_code", columnList = "code", unique = true),
    Index(name = "idx_hye_bank_info_president", columnList = "president_user_id")
])
class BankInfoEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "president_user_id", nullable = false) var presidentUserId: Long = 0,
    @Column(name = "code", length = 64, nullable = false, unique = true) var code: String = "",
    @Column(name = "name", length = 128, nullable = false) var name: String = "",
    @Column(name = "description", length = 1024, nullable = false) var description: String = "",
    @Column(name = "status", length = 32, nullable = false) var status: String = "ACTIVE",
    @Column(name = "star_level", nullable = false) var starLevel: Int = 1,
    @Column(name = "score", precision = 5, scale = 2, nullable = false) var score: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    @Column(name = "review_count", nullable = false) var reviewCount: Int = 0,
    @Column(name = "interest_policy", length = 32, nullable = false) var interestPolicy: String = "FIXED",
    @Column(name = "withdraw_enabled", nullable = false) var withdrawEnabled: Boolean = true,
    @Column(name = "suspended_until", columnDefinition = "DATETIME(3)") var suspendedUntil: Instant? = null,
    @Column(name = "bankrupt_at", columnDefinition = "DATETIME(3)") var bankruptAt: Instant? = null,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now(),
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
) {
    fun touch(now: Instant = EntityTime.now()) { updatedAt = now }
}
