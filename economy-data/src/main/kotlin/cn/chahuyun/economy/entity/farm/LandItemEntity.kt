package cn.chahuyun.economy.entity.farm

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "LandItemEntity")
@Table(name = "hye_land_item", indexes = [Index(name = "uk_hye_land_item_item", columnList = "item_id", unique = true), Index(name = "idx_hye_land_item_type", columnList = "land_type")])
class LandItemEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "item_id", nullable = false, unique = true) var itemId: Long = 0,
    @Column(name = "land_type", length = 32, nullable = false) var landType: String = "NORMAL",
    @Column(name = "base_fertilizer_boost", nullable = false) var baseFertilizerBoost: Int = 0,
    @Column(name = "base_nutrient", nullable = false) var baseNutrient: Int = 0,
    @Column(name = "base_luck", nullable = false) var baseLuck: Int = 0,
    @Column(name = "base_fertilizer_gather", nullable = false) var baseFertilizerGather: Int = 0,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now(),
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
)
