package cn.chahuyun.economy.entity.v2.privatebank

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "PrivateBankDepositEntityV2")
@Table(
    name = "hye_private_bank_deposit",
    indexes = [Index(name = "idx_hye_pb_deposit_bank_user", columnList = "bank_code,user_qq", unique = true)]
)
class PrivateBankDepositEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    var id: Long = 0,
    @Column(name = "bank_code", length = 128) var bankCode: String = "",
    @Column(name = "user_qq", nullable = false) var userQq: Long = 0,
    @Column(name = "principal", nullable = false) var principal: Double = 0.0,
    @Column(name = "created_at", nullable = false) var createdAt: Long = 0,
    @Column(name = "updated_at", nullable = false) var updatedAt: Long = 0
) : Serializable

@Entity(name = "PrivateBankReviewEntityV2")
@Table(name = "hye_private_bank_review", indexes = [Index(name = "idx_hye_pb_review_bank", columnList = "bank_code")])
class PrivateBankReviewEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    var id: Long = 0,
    @Column(name = "bank_code", length = 128) var bankCode: String = "",
    @Column(name = "user_qq", nullable = false) var userQq: Long = 0,
    @Column(name = "rating", nullable = false) var rating: Int = 5,
    @Lob @Column(name = "content", columnDefinition = "TEXT") var content: String = "",
    @Column(name = "created_at", nullable = false) var createdAt: Long = 0
) : Serializable

@Entity(name = "PrivateBankGovBondIssueEntityV2")
@Table(
    name = "hye_private_bank_gov_bond_issue",
    indexes = [Index(name = "idx_hye_pb_gov_issue_week", columnList = "week_key", unique = true)]
)
class PrivateBankGovBondIssueEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    var id: Long = 0,
    @Column(name = "week_key", length = 64) var weekKey: String = "",
    @Column(name = "rate_multiplier", nullable = false) var rateMultiplier: Double = 2.0,
    @Column(name = "lock_days", nullable = false) var lockDays: Int = 3,
    @Column(name = "total_limit", nullable = false) var totalLimit: Double = 0.0,
    @Column(name = "remaining", nullable = false) var remaining: Double = 0.0,
    @Column(name = "created_at", nullable = false) var createdAt: Long = 0,
    @Column(name = "code", length = 128) var code: String = ""
) : Serializable

@Entity(name = "PrivateBankGovBondHoldingEntityV2")
@Table(name = "hye_private_bank_gov_bond_holding", indexes = [Index(name = "idx_hye_pb_gov_holding_bank", columnList = "bank_code")])
class PrivateBankGovBondHoldingEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    var id: Long = 0,
    @Column(name = "bank_code", length = 128) var bankCode: String = "",
    @Column(name = "issue_id", nullable = false) var issueId: Int = 0,
    @Column(name = "principal", nullable = false) var principal: Double = 0.0,
    @Column(name = "rate_multiplier", nullable = false) var rateMultiplier: Double = 2.0,
    @Column(name = "lock_days", nullable = false) var lockDays: Int = 3,
    @Column(name = "bought_at", nullable = false) var boughtAt: Long = 0,
    @Column(name = "redeemed_at", nullable = false) var redeemedAt: Long = 0
) : Serializable

@Entity(name = "PrivateBankLoanOfferEntityV2")
@Table(name = "hye_private_bank_loan_offer", indexes = [Index(name = "idx_hye_pb_loan_offer_bank", columnList = "bank_code")])
class PrivateBankLoanOfferEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    var id: Long = 0,
    @Column(name = "bank_code", length = 128) var bankCode: String = "",
    @Column(name = "owner_qq", nullable = false) var ownerQq: Long = 0,
    @Column(name = "source", length = 32) var source: String = "LIQUIDITY",
    @Column(name = "total", nullable = false) var total: Double = 0.0,
    @Column(name = "remaining", nullable = false) var remaining: Double = 0.0,
    @Column(name = "interest", nullable = false) var interest: Int = 10,
    @Column(name = "term_days", nullable = false) var termDays: Int = 7,
    @Column(name = "enabled", nullable = false) var enabled: Boolean = true,
    @Column(name = "created_at", nullable = false) var createdAt: Long = 0
) : Serializable

@Entity(name = "PrivateBankLoanEntityV2")
@Table(
    name = "hye_private_bank_loan",
    indexes = [
        Index(name = "idx_hye_pb_loan_borrower", columnList = "borrower_qq"),
        Index(name = "idx_hye_pb_loan_repaid", columnList = "repaid_at")
    ]
)
class PrivateBankLoanEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    var id: Long = 0,
    @Column(name = "offer_id", nullable = false) var offerId: Int = 0,
    @Column(name = "bank_code", length = 128) var bankCode: String = "",
    @Column(name = "lender_qq", nullable = false) var lenderQq: Long = 0,
    @Column(name = "borrower_qq", nullable = false) var borrowerQq: Long = 0,
    @Column(name = "principal", nullable = false) var principal: Double = 0.0,
    @Column(name = "due_total", nullable = false) var dueTotal: Double = 0.0,
    @Column(name = "repaid_amount", nullable = false) var repaidAmount: Double = 0.0,
    @Column(name = "interest", nullable = false) var interest: Int = 10,
    @Column(name = "term_days", nullable = false) var termDays: Int = 7,
    @Column(name = "created_at", nullable = false) var createdAt: Long = 0,
    @Column(name = "due_at", nullable = false) var dueAt: Long = 0,
    @Column(name = "repaid_at", nullable = false) var repaidAt: Long = 0
) : Serializable

@Entity(name = "PrivateBankMainBankDebtEntityV2")
@Table(
    name = "hye_private_bank_main_bank_debt",
    indexes = [Index(name = "idx_hye_pb_main_debt_bank", columnList = "bank_code", unique = true)]
)
class PrivateBankMainBankDebtEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    var id: Long = 0,
    @Column(name = "bank_code", length = 128) var bankCode: String = "",
    @Column(name = "principal", nullable = false) var principal: Double = 0.0,
    @Column(name = "accrued_interest", nullable = false) var accruedInterest: Double = 0.0,
    @Column(name = "last_accrued_at", nullable = false) var lastAccruedAt: Long = 0,
    @Column(name = "created_at", nullable = false) var createdAt: Long = 0,
    @Column(name = "updated_at", nullable = false) var updatedAt: Long = 0,
    @Column(name = "repaid_at", nullable = false) var repaidAt: Long = 0,
) : Serializable

@Entity(name = "PrivateBankFoxBondEntityV2")
@Table(
    name = "hye_private_bank_fox_bond",
    indexes = [
        Index(name = "idx_hye_pb_fox_bond_code", columnList = "code", unique = true),
        Index(name = "idx_hye_pb_fox_bond_status", columnList = "status")
    ]
)
class PrivateBankFoxBondEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    var id: Long = 0,
    @Column(name = "code", length = 128) var code: String = "",
    @Column(name = "face_value", nullable = false) var faceValue: Double = 0.0,
    @Column(name = "base_rate", nullable = false) var baseRate: Double = 0.0,
    @Column(name = "term_days", nullable = false) var termDays: Int = 14,
    @Column(name = "bid_start_at", nullable = false) var bidStartAt: Long = 0,
    @Column(name = "bid_end_at", nullable = false) var bidEndAt: Long = 0,
    @Column(name = "status", length = 32) var status: String = "BIDDING",
    @Column(name = "winner_bank_code", length = 128) var winnerBankCode: String = "",
    @Column(name = "winner_bid_rate", nullable = false) var winnerBidRate: Double = 0.0,
    @Column(name = "winner_premium", nullable = false) var winnerPremium: Double = 0.0,
    @Column(name = "created_at", nullable = false) var createdAt: Long = 0
) : Serializable

@Entity(name = "PrivateBankFoxBondBidEntityV2")
@Table(
    name = "hye_private_bank_fox_bond_bid",
    indexes = [
        Index(name = "idx_hye_pb_fox_bid_code", columnList = "bond_code"),
        Index(name = "idx_hye_pb_fox_bid_bank", columnList = "bond_code,bank_code", unique = true)
    ]
)
class PrivateBankFoxBondBidEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    var id: Long = 0,
    @Column(name = "bond_code", length = 128) var bondCode: String = "",
    @Column(name = "bank_code", length = 128) var bankCode: String = "",
    @Column(name = "owner_qq", nullable = false) var ownerQq: Long = 0,
    @Column(name = "premium", nullable = false) var premium: Double = 0.0,
    @Column(name = "bid_rate", nullable = false) var bidRate: Double = 0.0,
    @Column(name = "created_at", nullable = false) var createdAt: Long = 0
) : Serializable

@Entity(name = "PrivateBankFoxBondHoldingEntityV2")
@Table(
    name = "hye_private_bank_fox_bond_holding",
    indexes = [
        Index(name = "idx_hye_pb_fox_holding_bank", columnList = "bank_code"),
        Index(name = "idx_hye_pb_fox_holding_redeemed", columnList = "redeemed_at")
    ]
)
class PrivateBankFoxBondHoldingEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    var id: Long = 0,
    @Column(name = "bond_code", length = 128) var bondCode: String = "",
    @Column(name = "bank_code", length = 128) var bankCode: String = "",
    @Column(name = "principal", nullable = false) var principal: Double = 0.0,
    @Column(name = "rate", nullable = false) var rate: Double = 0.0,
    @Column(name = "started_at", nullable = false) var startedAt: Long = 0,
    @Column(name = "due_at", nullable = false) var dueAt: Long = 0,
    @Column(name = "redeemed_at", nullable = false) var redeemedAt: Long = 0
) : Serializable
