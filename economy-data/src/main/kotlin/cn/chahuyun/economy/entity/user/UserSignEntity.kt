package cn.chahuyun.economy.entity.user

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "UserSignEntity")
@Table(name = "hye_user_sign", indexes = [Index(name = "uk_hye_user_sign_user", columnList = "user_id", unique = true)])
class UserSignEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "user_id", nullable = false, unique = true) var userId: Long = 0,
    @Column(name = "sign_time", columnDefinition = "DATETIME(3)") var signTime: Instant? = null,
    @Column(name = "continuous_days", nullable = false) var continuousDays: Int = 0,
    @Column(name = "last_broken_continuous_days", nullable = false) var lastBrokenContinuousDays: Int = 0,
    @Column(name = "max_continuous_days", nullable = false) var maxContinuousDays: Int = 0,
    @Column(name = "total_sign_days", nullable = false) var totalSignDays: Int = 0,
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
) {
    fun touch(now: Instant = EntityTime.now()) { updatedAt = now }
}
