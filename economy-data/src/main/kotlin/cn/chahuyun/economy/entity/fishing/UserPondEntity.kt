package cn.chahuyun.economy.entity.fishing

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "UserPondEntity")
@Table(name = "hye_user_pond", indexes = [
    Index(name = "idx_hye_user_pond_user_created", columnList = "user_id,created_at"),
    Index(name = "uk_hye_user_pond_user_name", columnList = "user_id,name", unique = true)
])
class UserPondEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "user_id", nullable = false) var userId: Long = 0,
    @Column(name = "name", length = 128, nullable = false) var name: String = "",
    @Column(name = "level", nullable = false) var level: Int = 1,
    @Column(name = "fee_rate_permille", nullable = false) var feeRatePermille: Int = 0,
    @Column(name = "total_fishing_count", nullable = false) var totalFishingCount: Int = 0,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now(),
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
) {
    fun touch(now: Instant = EntityTime.now()) { updatedAt = now }
}
