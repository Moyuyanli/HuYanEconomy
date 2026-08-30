package cn.chahuyun.economy.entity.farm

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity(name = "UserFarmEntity")
@Table(name = "hye_user_farm", indexes = [Index(name = "uk_hye_user_farm_user", columnList = "user_id", unique = true)])
class UserFarmEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "user_id", nullable = false, unique = true) var userId: Long = 0,
    @Column(name = "level", nullable = false) var level: Int = 1,
    @Column(name = "total_plant_count", nullable = false) var totalPlantCount: Int = 0,
    @Column(name = "total_income", precision = 19, scale = 2, nullable = false) var totalIncome: BigDecimal = BigDecimal.ZERO,
    @Column(name = "daily_action_used", nullable = false) var dailyActionUsed: Int = 0,
    @Column(name = "daily_action_limit", nullable = false) var dailyActionLimit: Int = 0,
    @Column(name = "action_reset_at", nullable = false, columnDefinition = "DATETIME(3)") var actionResetAt: Instant = EntityTime.now(),
    @Column(name = "today_water_count", nullable = false) var todayWaterCount: Int = 0,
    @Column(name = "water_date", length = 10) var waterDate: String? = null,
    @Column(name = "shield_until", columnDefinition = "DATETIME(3)") var shieldUntil: Instant? = null,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now(),
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
)
