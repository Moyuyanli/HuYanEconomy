package cn.chahuyun.economy.entity.v2.bank

import jakarta.persistence.*
import java.io.Serializable

/**
 * V2 bank entity.
 */
@Entity(name = "BankEntityV2")
@Table(
    name = "hye_bank",
    indexes = [
        Index(name = "idx_hye_bank_code", columnList = "code", unique = true),
        Index(name = "idx_hye_bank_owner_qq", columnList = "owner_qq")
    ]
)
class BankEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "code", length = 64)
    var code: String = "",

    @Column(name = "name", length = 128)
    var name: String = "",

    @Column(name = "description", length = 1024)
    var description: String = "",

    @Column(name = "owner_qq", nullable = false)
    var qq: Long = 0,

    @Column(name = "interest_switch", nullable = false)
    var interestSwitch: Boolean = false,

    @Column(name = "reg_time", nullable = false)
    var regTime: Long = 0,

    @Column(name = "reg_total", nullable = false)
    var regTotal: Double = 0.0,

    @Column(name = "total", nullable = false)
    var total: Double = 0.0,

    @Column(name = "interest", nullable = false)
    var interest: Int = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0
) : Serializable
