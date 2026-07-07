package cn.chahuyun.economy.entity.v2.user

import jakarta.persistence.*
import java.io.Serializable

/**
 * V2 user backpack item entity.
 */
@Entity(name = "UserBackpackEntityV2")
@Table(
    name = "hye_user_backpack",
    indexes = [
        Index(name = "idx_hye_user_backpack_user_key", columnList = "user_key"),
        Index(name = "idx_hye_user_backpack_prop_id", columnList = "prop_id")
    ]
)
class UserBackpackEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "user_key", length = 128)
    var userKey: String = "",

    @Column(name = "prop_code", length = 128)
    var propCode: String = "",

    @Column(name = "prop_kind", length = 128)
    var propKind: String = "",

    @Column(name = "prop_id", nullable = false)
    var propId: Long = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0
) : Serializable
