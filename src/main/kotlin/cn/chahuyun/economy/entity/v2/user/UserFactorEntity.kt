package cn.chahuyun.economy.entity.v2.user

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "UserFactorEntityV2")
@Table(
    name = "hye_user_factor",
    indexes = [Index(name = "idx_hye_user_factor_user_id", columnList = "user_id", unique = true)]
)
class UserFactorEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "irritable", nullable = false)
    var irritable: Double = 0.3,

    @Column(name = "force_value", nullable = false)
    var force: Double = 0.1,

    @Column(name = "dodge", nullable = false)
    var dodge: Double = 0.1,

    @Column(name = "resistance", nullable = false)
    var resistance: Double = 0.3,

    @Lob
    @Column(name = "buff", columnDefinition = "TEXT")
    var buff: String = "[]",

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0
) : Serializable
