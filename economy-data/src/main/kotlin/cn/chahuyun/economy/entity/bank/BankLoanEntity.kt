package cn.chahuyun.economy.entity.bank

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity(name = "BankLoanConfigEntity")
@Table(name = "hye_bank_loan_config", indexes = [Index(name = "idx_hye_bank_loan_config_bank_status", columnList = "bank_id,status")])
class BankLoanConfigEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "bank_id", nullable = false) var bankId: Long = 0,
    @Column(name = "min_loan_amount", precision = 19, scale = 2, nullable = false) var minLoanAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "max_loan_limit", precision = 19, scale = 2, nullable = false) var maxLoanLimit: BigDecimal = BigDecimal.ZERO,
    @Column(name = "interest_rate", precision = 9, scale = 6, nullable = false) var interestRate: BigDecimal = BigDecimal.ZERO,
    @Column(name = "term_days", nullable = false) var termDays: Int = 7,
    @Column(name = "status", length = 32, nullable = false) var status: String = "ACTIVE",
    @Column(name = "effective_from", nullable = false, columnDefinition = "DATETIME(3)") var effectiveFrom: Instant = EntityTime.now(),
    @Column(name = "effective_until", columnDefinition = "DATETIME(3)") var effectiveUntil: Instant? = null,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now(),
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
)

@Entity(name = "BankLoanEntity")
@Table(name = "hye_bank_loan_user", indexes = [
    Index(name = "idx_hye_bank_loan_user_status", columnList = "user_id,status"),
    Index(name = "idx_hye_bank_loan_bank_status", columnList = "bank_id,status"),
    Index(name = "idx_hye_bank_loan_due", columnList = "due_time")
])
class BankLoanEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "bank_id", nullable = false) var bankId: Long = 0,
    @Column(name = "loan_config_id", nullable = false) var loanConfigId: Long = 0,
    @Column(name = "user_id", nullable = false) var userId: Long = 0,
    @Column(name = "loan_amount", precision = 19, scale = 2, nullable = false) var loanAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "interest_rate", precision = 9, scale = 6, nullable = false) var interestRate: BigDecimal = BigDecimal.ZERO,
    @Column(name = "due_total", precision = 19, scale = 2, nullable = false) var dueTotal: BigDecimal = BigDecimal.ZERO,
    @Column(name = "repaid_amount", precision = 19, scale = 2, nullable = false) var repaidAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "outstanding_amount", precision = 19, scale = 2, nullable = false) var outstandingAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "status", length = 32, nullable = false) var status: String = "ACTIVE",
    @Column(name = "loan_time", nullable = false, columnDefinition = "DATETIME(3)") var loanTime: Instant = EntityTime.now(),
    @Column(name = "due_time", nullable = false, columnDefinition = "DATETIME(3)") var dueTime: Instant = EntityTime.now(),
    @Column(name = "repaid_time", columnDefinition = "DATETIME(3)") var repaidTime: Instant? = null,
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
)
