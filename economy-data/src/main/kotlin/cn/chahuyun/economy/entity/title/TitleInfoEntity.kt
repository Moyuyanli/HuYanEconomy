package cn.chahuyun.economy.entity.title

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "NewTitleInfoEntity")
@Table(name = "hye_title_info", indexes = [
    Index(name = "idx_hye_title_user_id", columnList = "user_id"),
    Index(name = "idx_hye_title_code", columnList = "code")
])
class TitleInfoEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "user_id", nullable = false) var userId: Long = 0,
    @Column(name = "code", length = 128, nullable = false) var code: String = "",
    @Column(name = "name", length = 128, nullable = false) var name: String = "",
    @Column(name = "status", nullable = false) var status: Boolean = false,
    @Column(name = "title", length = 256, nullable = false) var title: String = "",
    @Column(name = "impact_name", nullable = false) var impactName: Boolean = false,
    @Column(name = "gradient", nullable = false) var gradient: Boolean = false,
    @Column(name = "start_color", length = 32, nullable = false) var startColor: String = "",
    @Column(name = "end_color", length = 32, nullable = false) var endColor: String = "",
    @Column(name = "due_time", columnDefinition = "DATETIME(3)") var dueTime: Instant? = null,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now(),
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
) {
    fun touch(now: Instant = EntityTime.now()) { updatedAt = now }
}
