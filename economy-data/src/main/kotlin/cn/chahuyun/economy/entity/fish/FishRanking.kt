package cn.chahuyun.economy.entity.fish

import jakarta.persistence.*
import java.io.Serializable
import java.util.*

/**
 * Fish ranking V1 persistence entity.
 */
@Entity(name = "FishRanking")
@Table
class FishRanking(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    var qq: Long = 0,

    var name: String? = null,

    var dimensions: Int = 0,

    var money: Double = 0.0,

    var fishRodLevel: Int = 0,

    var date: Date? = null,

    @ManyToOne(targetEntity = Fish::class, fetch = FetchType.EAGER)
    @JoinColumn(name = "FishId")
    var fish: Fish? = null,

    @ManyToOne(targetEntity = FishPond::class, fetch = FetchType.EAGER)
    @JoinColumn(name = "FishPondId")
    var fishPond: FishPond? = null
) : Serializable {

    constructor(
        qq: Long,
        name: String?,
        dimensions: Int,
        money: Double,
        fishRodLevel: Int,
        fish: Fish?,
        fishPond: FishPond?,
    ) : this(
        qq = qq,
        name = name,
        dimensions = dimensions,
        money = money,
        fishRodLevel = fishRodLevel,
        fish = fish,
        fishPond = fishPond,
        date = Date()
    )
}
