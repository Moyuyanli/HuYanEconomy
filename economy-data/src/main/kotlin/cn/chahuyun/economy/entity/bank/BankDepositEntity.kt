package cn.chahuyun.economy.entity.bank

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity(name = "BankDepositEntity")
@Table(name = "hye_bank_deposit_user", indexes = [
    Index(name = "uk_hye_bank_deposit_bank_user", columnList = "bank_id,user_id", unique = true),
    Index(name = "idx_hye_bank_deposit_user", columnList = "user_id")
])
class BankDepositEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "bank_id", nullable = false) var bankId: Long = 0,
    @Column(name = "user_id", nullable = false) var userId: Long = 0,
    @Column(name = "deposit_amount", precision = 19, scale = 2, nullable = false) var depositAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "interest_earned", precision = 19, scale = 2, nullable = false) var interestEarned: BigDecimal = BigDecimal.ZERO,
    @Column(name = "interest_rate_snapshot", precision = 9, scale = 6, nullable = false) var interestRateSnapshot: BigDecimal = BigDecimal.ZERO,
    @Column(name = "last_interest_settled_at", columnDefinition = "DATETIME(3)") var lastInterestSettledAt: Instant? = null,
    @Column(name = "status", length = 32, nullable = false) var status: String = "ACTIVE",
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now(),
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
)
