package cn.chahuyun.economy.entity.fish

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.io.Serializable

/**
 * Player fish info V1 persistence entity.
 */
@Table
@Entity(name = "FishInfo")
class FishInfo(
    @Id
    var id: Long = 0,

    var qq: Long = 0,

    var isFishRod: Boolean = false,

    var status: Boolean = false,

    var rodLevel: Int = 0,

    var defaultFishPond: String? = null
) : Serializable {

    constructor(qq: Long, group: Long) : this(
        id = qq,
        qq = qq,
        isFishRod = false,
        status = false,
        rodLevel = 0,
        defaultFishPond = "g-$group"
    )
}
