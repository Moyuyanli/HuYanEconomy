package cn.chahuyun.economy.entity.v2.fish

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "FishEntityV2")
@Table(
    name = "hye_fish",
    indexes = [
        Index(name = "idx_hye_fish_level", columnList = "level"),
        Index(name = "idx_hye_fish_name", columnList = "name")
    ]
)
class FishEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "level", nullable = false)
    var level: Int = 0,

    @Column(name = "name", length = 128)
    var name: String = "",

    @Column(name = "description", length = 1024)
    var description: String = "",

    @Column(name = "price", nullable = false)
    var price: Int = 0,

    @Column(name = "dimensions_min", nullable = false)
    var dimensionsMin: Int = 0,

    @Column(name = "dimensions_max", nullable = false)
    var dimensionsMax: Int = 0,

    @Column(name = "dimensions_1", nullable = false)
    var dimensions1: Int = 0,

    @Column(name = "dimensions_2", nullable = false)
    var dimensions2: Int = 0,

    @Column(name = "dimensions_3", nullable = false)
    var dimensions3: Int = 0,

    @Column(name = "dimensions_4", nullable = false)
    var dimensions4: Int = 0,

    @Column(name = "difficulty", nullable = false)
    var difficulty: Int = 0,

    @Column(name = "special", nullable = false)
    var special: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0
) : Serializable
