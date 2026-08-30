package cn.chahuyun.economy.entity.farm

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity(name = "FarmEventLogEntity")
@Table(name = "hye_farm_event_log", indexes = [Index(name = "idx_hye_farm_event_farm_time", columnList = "farm_id,created_at"), Index(name = "idx_hye_farm_event_user_time", columnList = "user_id,created_at"), Index(name = "idx_hye_farm_event_type", columnList = "event_type")])
class FarmEventLogEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "farm_id", nullable = false) var farmId: Long = 0,
    @Column(name = "user_id", nullable = false) var userId: Long = 0,
    @Column(name = "operator_user_id") var operatorUserId: Long? = null,
    @Column(name = "land_id") var landId: Long? = null,
    @Column(name = "planting_id") var plantingId: Long? = null,
    @Column(name = "event_type", length = 32, nullable = false) var eventType: String = "PLANT",
    @Column(name = "target_id") var targetId: Long? = null,
    @Column(name = "quantity", nullable = false) var quantity: Int = 0,
    @Column(name = "amount", precision = 19, scale = 2, nullable = false) var amount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now()
)
