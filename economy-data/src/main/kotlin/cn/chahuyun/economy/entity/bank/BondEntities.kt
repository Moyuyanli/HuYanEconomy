package cn.chahuyun.economy.entity.bank

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity(name = "NationalBondEntity")
@Table(name = "hye_national_bond", indexes = [Index(name = "uk_hye_national_bond_code", columnList = "code", unique = true)])
class NationalBondEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "code", length = 64, nullable = false, unique = true) var code: String = "",
    @Column(name = "total_amount", precision = 19, scale = 2, nullable = false) var totalAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "remaining_amount", precision = 19, scale = 2, nullable = false) var remainingAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "interest_rate", precision = 9, scale = 6, nullable = false) var interestRate: BigDecimal = BigDecimal.ZERO,
    @Column(name = "status", length = 32, nullable = false) var status: String = "AVAILABLE",
    @Column(name = "issue_time", nullable = false, columnDefinition = "DATETIME(3)") var issueTime: Instant = EntityTime.now(),
    @Column(name = "maturity_at", columnDefinition = "DATETIME(3)") var maturityAt: Instant? = null,
    @Column(name = "redeemed_at", columnDefinition = "DATETIME(3)") var redeemedAt: Instant? = null,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now(),
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
)

@Entity(name = "BankNationalBondHoldingEntity")
@Table(name = "hye_bank_national_bond_holding", indexes = [Index(name = "uk_hye_bank_national_holding", columnList = "bank_id,national_bond_id", unique = true)])
class BankNationalBondHoldingEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "bank_id", nullable = false) var bankId: Long = 0,
    @Column(name = "national_bond_id", nullable = false) var nationalBondId: Long = 0,
    @Column(name = "holding_amount", precision = 19, scale = 2, nullable = false) var holdingAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "interest_rate_snapshot", precision = 9, scale = 6, nullable = false) var interestRateSnapshot: BigDecimal = BigDecimal.ZERO,
    @Column(name = "maturity_at", columnDefinition = "DATETIME(3)") var maturityAt: Instant? = null,
    @Column(name = "redeemed_at", columnDefinition = "DATETIME(3)") var redeemedAt: Instant? = null,
    @Column(name = "status", length = 32, nullable = false) var status: String = "HOLDING",
    @Column(name = "bought_at", nullable = false, columnDefinition = "DATETIME(3)") var boughtAt: Instant = EntityTime.now()
)

@Entity(name = "FoxBondEntity")
@Table(name = "hye_fox_bond", indexes = [Index(name = "uk_hye_fox_bond_code", columnList = "code", unique = true), Index(name = "idx_hye_fox_bond_status", columnList = "status")])
class FoxBondEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "code", length = 64, nullable = false, unique = true) var code: String = "",
    @Column(name = "face_value", precision = 19, scale = 2, nullable = false) var faceValue: BigDecimal = BigDecimal.ZERO,
    @Column(name = "base_rate", precision = 9, scale = 6, nullable = false) var baseRate: BigDecimal = BigDecimal.ZERO,
    @Column(name = "term_days", nullable = false) var termDays: Int = 14,
    @Column(name = "bid_start_at", nullable = false, columnDefinition = "DATETIME(3)") var bidStartAt: Instant = EntityTime.now(),
    @Column(name = "bid_end_at", nullable = false, columnDefinition = "DATETIME(3)") var bidEndAt: Instant = EntityTime.now(),
    @Column(name = "status", length = 32, nullable = false) var status: String = "BIDDING",
    @Column(name = "winner_bank_id") var winnerBankId: Long? = null,
    @Column(name = "winner_bid_rate", precision = 9, scale = 6) var winnerBidRate: BigDecimal? = null,
    @Column(name = "winner_premium", precision = 19, scale = 2) var winnerPremium: BigDecimal? = null,
    @Column(name = "maturity_at", columnDefinition = "DATETIME(3)") var maturityAt: Instant? = null,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now()
)

@Entity(name = "BankFoxBondAuctionEntity")
@Table(name = "hye_bank_fox_bond_auction", indexes = [Index(name = "uk_hye_fox_auction_bidder", columnList = "fox_bond_id,bank_id", unique = true), Index(name = "idx_hye_fox_auction_bond", columnList = "fox_bond_id")])
class BankFoxBondAuctionEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "fox_bond_id", nullable = false) var foxBondId: Long = 0,
    @Column(name = "bank_id", nullable = false) var bankId: Long = 0,
    @Column(name = "bid_rate", precision = 9, scale = 6, nullable = false) var bidRate: BigDecimal = BigDecimal.ZERO,
    @Column(name = "premium", precision = 19, scale = 2, nullable = false) var premium: BigDecimal = BigDecimal.ZERO,
    @Column(name = "status", length = 32, nullable = false) var status: String = "BIDDING",
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now(),
    @Column(name = "settled_at", columnDefinition = "DATETIME(3)") var settledAt: Instant? = null
)

@Entity(name = "BankFoxBondHoldingEntity")
@Table(name = "hye_bank_fox_bond_holding", indexes = [Index(name = "idx_hye_fox_holding_bank", columnList = "bank_id"), Index(name = "idx_hye_fox_holding_due", columnList = "due_at")])
class BankFoxBondHoldingEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "fox_bond_id", nullable = false) var foxBondId: Long = 0,
    @Column(name = "bank_id", nullable = false) var bankId: Long = 0,
    @Column(name = "principal", precision = 19, scale = 2, nullable = false) var principal: BigDecimal = BigDecimal.ZERO,
    @Column(name = "rate_snapshot", precision = 9, scale = 6, nullable = false) var rateSnapshot: BigDecimal = BigDecimal.ZERO,
    @Column(name = "status", length = 32, nullable = false) var status: String = "HOLDING",
    @Column(name = "started_at", nullable = false, columnDefinition = "DATETIME(3)") var startedAt: Instant = EntityTime.now(),
    @Column(name = "due_at", nullable = false, columnDefinition = "DATETIME(3)") var dueAt: Instant = EntityTime.now(),
    @Column(name = "redeemed_at", columnDefinition = "DATETIME(3)") var redeemedAt: Instant? = null
)
