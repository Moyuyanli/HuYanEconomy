package cn.chahuyun.economy.entity.bank

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity(name = "BankAssetLedgerEntity")
@Table(name = "hye_bank_asset_ledger", indexes = [
    Index(name = "idx_hye_bank_ledger_bank_time", columnList = "bank_id,created_at"),
    Index(name = "idx_hye_bank_ledger_business", columnList = "business_type,business_id")
])
class BankAssetLedgerEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "bank_id", nullable = false) var bankId: Long = 0,
    @Column(name = "asset_type", length = 32, nullable = false) var assetType: String = "LIQUIDITY",
    @Column(name = "amount", precision = 19, scale = 2, nullable = false) var amount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "balance_after", precision = 19, scale = 2, nullable = false) var balanceAfter: BigDecimal = BigDecimal.ZERO,
    @Column(name = "business_type", length = 64, nullable = false) var businessType: String = "",
    @Column(name = "business_id", nullable = false) var businessId: Long = 0,
    @Column(name = "operator_user_id") var operatorUserId: Long? = null,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now()
)
