package cn.chahuyun.economy.entity.farm

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "FarmLandEntity")
@Table(name = "hye_farm_land", indexes = [Index(name = "uk_hye_farm_land_slot", columnList = "farm_id,slot_no", unique = true), Index(name = "idx_hye_farm_land_farm", columnList = "farm_id")])
class FarmLandEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "farm_id", nullable = false) var farmId: Long = 0,
    @Column(name = "slot_no", nullable = false) var slotNo: Int = 0,
    @Column(name = "status", length = 32, nullable = false) var status: String = "LOCKED",
    @Column(name = "land_item_id") var landItemId: Long? = null,
    @Column(name = "fertilizer_boost", nullable = false) var fertilizerBoost: Int = 0,
    @Column(name = "nutrient", nullable = false) var nutrient: Int = 0,
    @Column(name = "luck", nullable = false) var luck: Int = 0,
    @Column(name = "fertilizer_gather", nullable = false) var fertilizerGather: Int = 0,
    @Column(name = "is_auto_harvest", nullable = false) var autoHarvest: Boolean = false,
    @Version @Column(name = "version", nullable = false) var version: Long = 0,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now(),
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
)
