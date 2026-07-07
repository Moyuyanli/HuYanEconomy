package cn.chahuyun.economy.entity.v2.user

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "UserStatusEntityV2")
@Table(
    name = "hye_user_status",
    indexes = [Index(name = "idx_hye_user_status_user_id", columnList = "user_id", unique = true)]
)
class UserStatusEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "place", length = 64, nullable = false)
    var place: String = "HOME",

    @Column(name = "recovery_time", nullable = false)
    var recoveryTime: Int = 0,

    @Column(name = "start_time", nullable = false)
    var startTime: Long = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0
) : Serializable
