package cn.chahuyun.economy.entity.bank

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity(name = "BankAssetEntity")
@Table(name = "hye_bank_asset", indexes = [Index(name = "uk_hye_bank_asset_bank", columnList = "bank_id", unique = true)])
class BankAssetEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "bank_id", nullable = false, unique = true) var bankId: Long = 0,
    @Column(name = "total_assets", precision = 19, scale = 2, nullable = false) var totalAssets: BigDecimal = BigDecimal.ZERO,
    @Column(name = "liquidity_pool", precision = 19, scale = 2, nullable = false) var liquidityPool: BigDecimal = BigDecimal.ZERO,
    @Column(name = "reserve_pool", precision = 19, scale = 2, nullable = false) var reservePool: BigDecimal = BigDecimal.ZERO,
    @Column(name = "lending_pool", precision = 19, scale = 2, nullable = false) var lendingPool: BigDecimal = BigDecimal.ZERO,
    @Version @Column(name = "version", nullable = false) var version: Long = 0,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now(),
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
)
