package cn.chahuyun.economy.entity.bank

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "BankReviewEntity")
@Table(name = "hye_bank_review", indexes = [
    Index(name = "uk_hye_bank_review_user_bank", columnList = "user_id,bank_id", unique = true),
    Index(name = "idx_hye_bank_review_bank_score", columnList = "bank_id,score")
])
class BankReviewEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "user_id", nullable = false) var userId: Long = 0,
    @Column(name = "bank_id", nullable = false) var bankId: Long = 0,
    @Column(name = "score", nullable = false) var score: Int = 5,
    @Lob @Column(name = "comment", length = 1024, nullable = false) var comment: String = "",
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now(),
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
)
