package cn.chahuyun.economy.entity.farm

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "FarmPlantingEntity")
@Table(name = "hye_farm_planting", indexes = [Index(name = "uk_hye_farm_planting_land", columnList = "land_id", unique = true), Index(name = "idx_hye_farm_planting_crop", columnList = "crop_id")])
class FarmPlantingEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "land_id", nullable = false, unique = true) var landId: Long = 0,
    @Column(name = "crop_id", nullable = false) var cropId: Long = 0,
    @Column(name = "plant_time", nullable = false, columnDefinition = "DATETIME(3)") var plantTime: Instant = EntityTime.now(),
    @Column(name = "next_mature_at", nullable = false, columnDefinition = "DATETIME(3)") var nextMatureAt: Instant = EntityTime.now(),
    @Column(name = "status", length = 32, nullable = false) var status: String = "GROWING",
    @Column(name = "harvestable_quantity", nullable = false) var harvestableQuantity: Int = 0,
    @Column(name = "remaining_quantity", nullable = false) var remainingQuantity: Int = 0,
    @Column(name = "current_season", nullable = false) var currentSeason: Int = 1,
    @Column(name = "stolen_season", nullable = false) var stolenSeason: Int = 0,
    @Column(name = "stolen_amount", nullable = false) var stolenAmount: Int = 0,
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
)
