package cn.chahuyun.economy.entity.v2.fish

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "FishInfoEntityV2")
@Table(
    name = "hye_fish_info",
    indexes = [Index(name = "idx_hye_fish_info_qq", columnList = "qq", unique = true)]
)
class FishInfoEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "qq", nullable = false)
    var qq: Long = 0,

    @Column(name = "fish_rod", nullable = false)
    var isFishRod: Boolean = false,

    @Column(name = "status", nullable = false)
    var status: Boolean = false,

    @Column(name = "rod_level", nullable = false)
    var rodLevel: Int = 0,

    @Column(name = "default_fish_pond", length = 128)
    var defaultFishPond: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0
) : Serializable
