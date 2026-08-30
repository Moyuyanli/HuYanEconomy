package cn.chahuyun.economy.entity.farm

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity(name = "CropEntity")
@Table(name = "hye_crop", indexes = [
    Index(name = "uk_hye_crop_code", columnList = "code", unique = true),
    Index(name = "idx_hye_crop_level", columnList = "level")
])
class CropEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "code", length = 64, nullable = false, unique = true) var code: String = "",
    @Column(name = "level", nullable = false) var level: Int = 1,
    @Column(name = "name", length = 64, nullable = false) var name: String = "",
    @Column(name = "description", length = 512, nullable = false) var description: String = "",
    @Column(name = "emoji", length = 16, nullable = false) var emoji: String = "",
    @Column(name = "seed_item_id", nullable = false) var seedItemId: Long = 0,
    @Column(name = "fruit_item_id", nullable = false) var fruitItemId: Long = 0,
    @Column(name = "seed_price", precision = 19, scale = 2, nullable = false) var seedPrice: BigDecimal = BigDecimal.ZERO,
    @Column(name = "fruit_unit_price", precision = 19, scale = 2, nullable = false) var fruitUnitPrice: BigDecimal = BigDecimal.ZERO,
    @Column(name = "nutrient_required", nullable = false) var nutrientRequired: Int = 0,
    @Column(name = "first_mature_seconds", nullable = false) var firstMatureSeconds: Int = 0,
    @Column(name = "regrow_seconds", nullable = false) var regrowSeconds: Int = 0,
    @Column(name = "yield_per_season", nullable = false) var yieldPerSeason: Int = 1,
    @Column(name = "total_seasons", nullable = false) var totalSeasons: Int = 1,
    @Column(name = "status", length = 32, nullable = false) var status: String = "ACTIVE",
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now(),
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
)
