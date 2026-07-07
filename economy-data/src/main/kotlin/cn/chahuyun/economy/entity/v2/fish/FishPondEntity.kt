package cn.chahuyun.economy.entity.v2.fish

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "FishPondEntityV2")
@Table(
    name = "hye_fish_pond",
    indexes = [
        Index(name = "idx_hye_fish_pond_code", columnList = "code", unique = true),
        Index(name = "idx_hye_fish_pond_admin", columnList = "admin")
    ]
)
class FishPondEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "code", length = 128)
    var code: String = "",

    @Column(name = "admin", nullable = false)
    var admin: Long = 0,

    @Column(name = "pond_type", nullable = false)
    var pondType: Int = 0,

    @Column(name = "name", length = 128)
    var name: String = "",

    @Column(name = "description", length = 1024)
    var description: String = "",

    @Column(name = "pond_level", nullable = false)
    var pondLevel: Int = 0,

    @Column(name = "min_level", nullable = false)
    var minLevel: Int = 0,

    @Column(name = "rebate", nullable = false)
    var rebate: Double = 0.05,

    @Column(name = "number", nullable = false)
    var number: Int = 0,

    @Column(name = "fish_count", nullable = false)
    var fishCount: Int = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0
) : Serializable
