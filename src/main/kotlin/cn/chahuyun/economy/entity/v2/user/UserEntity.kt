package cn.chahuyun.economy.entity.v2.user

import jakarta.persistence.*
import java.io.Serializable

/**
 * V2 user core entity.
 *
 * Backpack data stays in the independent backpack module for this migration slice.
 */
@Entity(name = "UserEntityV2")
@Table(
    name = "hye_user",
    indexes = [
        Index(name = "idx_hye_user_key", columnList = "user_key", unique = true),
        Index(name = "idx_hye_user_qq", columnList = "qq", unique = true),
        Index(name = "idx_hye_user_funding", columnList = "funding")
    ]
)
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "user_key", length = 128)
    var userKey: String? = null,

    @Column(name = "qq", nullable = false)
    var qq: Long = 0,

    @Column(name = "name", length = 128)
    var name: String = "",

    @Column(name = "register_group", nullable = false)
    var registerGroup: Long = 0,

    @Column(name = "register_time", nullable = false)
    var registerTime: Long = 0,

    @Column(name = "signed", nullable = false)
    var sign: Boolean = false,

    @Column(name = "sign_time", nullable = false)
    var signTime: Long = 0,

    @Column(name = "sign_number", nullable = false)
    var signNumber: Int = 0,

    @Column(name = "old_sign_number", nullable = false)
    var oldSignNumber: Int = 0,

    @Column(name = "sign_earnings", nullable = false)
    var signEarnings: Double = 0.0,

    @Column(name = "bank_earnings", nullable = false)
    var bankEarnings: Double = 0.0,

    @Column(name = "default_private_bank_code", length = 64)
    var defaultPrivateBankCode: String? = null,

    @Column(name = "funding", length = 128)
    var funding: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0
) : Serializable
