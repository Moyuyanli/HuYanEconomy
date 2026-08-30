package cn.chahuyun.economy.entity.fishing

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity(name = "UserFishingInfoEntity")
@Table(name = "hye_user_fishing_info", indexes = [
    Index(name = "uk_hye_fishing_info_user", columnList = "user_id", unique = true)
])
class UserFishingInfoEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "user_id", nullable = false, unique = true) var userId: Long = 0,
    @Column(name = "rod_level", nullable = false) var rodLevel: Int = 1,
    @Column(name = "default_pond_id") var defaultPondId: Long? = null,
    @Column(name = "default_bait_item_id") var defaultBaitItemId: Long? = null,
    @Column(name = "total_fishing_count", nullable = false) var totalFishingCount: Int = 0,
    @Column(name = "max_fish_size", nullable = false) var maxFishSize: Int = 0,
    @Column(name = "max_fish_price", precision = 19, scale = 2, nullable = false) var maxFishPrice: BigDecimal = BigDecimal.ZERO,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now(),
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
) {
    fun touch(now: Instant = EntityTime.now()) { updatedAt = now }
}
