package cn.chahuyun.economy.entity.fishing

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

/** Global fish configuration. Result rows store immutable snapshots separately. */
@Entity(name = "FishEntity")
@Table(name = "hye_fish", indexes = [
    Index(name = "idx_hye_fish_level", columnList = "level"),
    Index(name = "idx_hye_fish_name", columnList = "name")
])
class FishEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "level", nullable = false) var level: Int = 1,
    @Column(name = "name", length = 128, nullable = false) var name: String = "",
    @Column(name = "description", length = 1024, nullable = false) var description: String = "",
    @Column(name = "price", precision = 19, scale = 2, nullable = false) var price: BigDecimal = BigDecimal.ZERO,
    @Column(name = "dimensions_min", nullable = false) var dimensionsMin: Int = 0,
    @Column(name = "dimensions_max", nullable = false) var dimensionsMax: Int = 0,
    @Column(name = "dimensions_1", nullable = false) var dimensions1: Int = 0,
    @Column(name = "dimensions_2", nullable = false) var dimensions2: Int = 0,
    @Column(name = "dimensions_3", nullable = false) var dimensions3: Int = 0,
    @Column(name = "dimensions_4", nullable = false) var dimensions4: Int = 0,
    @Column(name = "technique", nullable = false) var technique: Int = 0,
    @Column(name = "special", nullable = false) var special: Boolean = false,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now(),
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
) {
    fun touch(now: Instant = EntityTime.now()) { updatedAt = now }
}
