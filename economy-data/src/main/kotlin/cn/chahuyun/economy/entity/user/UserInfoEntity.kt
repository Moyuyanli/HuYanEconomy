package cn.chahuyun.economy.entity.user

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "UserInfoEntity")
@Table(name = "hye_user_info", indexes = [Index(name = "idx_hye_user_key", columnList = "user_key")])
class UserInfoEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    @Column(name = "qq", nullable = false, unique = true)
    var qq: Long = 0,
    @Column(name = "user_key", length = 16, nullable = false, unique = true)
    var userKey: String = "",
    @Column(name = "name", length = 64, nullable = false)
    var name: String = "",
    @Column(name = "register_group", length = 128, nullable = false)
    var registerGroup: String = "",
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)")
    var createdAt: Instant = EntityTime.now(),
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)")
    var updatedAt: Instant = EntityTime.now()
) {
    fun touch(now: Instant = EntityTime.now()) { updatedAt = now }
}
