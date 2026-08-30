package cn.chahuyun.economy.entity.fishing

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity(name = "FishingLogEntity")
@Table(name = "hye_fishing_log", indexes = [
    Index(name = "idx_hye_fishing_log_user_created", columnList = "user_id,created_at"),
    Index(name = "idx_hye_fishing_log_fish_created", columnList = "fish_id,created_at"),
    Index(name = "idx_hye_fishing_log_pond_created", columnList = "pond_id,created_at")
])
class FishingLogEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "user_id", nullable = false) var userId: Long = 0,
    @Column(name = "fish_id", nullable = false) var fishId: Long = 0,
    @Column(name = "pond_id", nullable = false) var pondId: Long = 0,
    @Column(name = "bait_item_id") var baitItemId: Long? = null,
    @Column(name = "rod_backpack_id") var rodBackpackId: Long? = null,
    @Column(name = "rod_level", nullable = false) var rodLevel: Int = 1,
    @Column(name = "size", nullable = false) var size: Int = 0,
    @Column(name = "level", nullable = false) var level: Int = 0,
    @Column(name = "price", precision = 19, scale = 2, nullable = false) var price: BigDecimal = BigDecimal.ZERO,
    @Column(name = "fee_amount", precision = 19, scale = 2, nullable = false) var feeAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "net_income", precision = 19, scale = 2, nullable = false) var netIncome: BigDecimal = BigDecimal.ZERO,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now()
)
