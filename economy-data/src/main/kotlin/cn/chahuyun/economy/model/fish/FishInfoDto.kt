package cn.chahuyun.economy.model.fish

import kotlinx.serialization.Serializable

/**
 * Player fish info data transfer object.
 */
@Serializable
data class FishInfoDto(
    var id: Long = 0,
    var qq: Long = 0,
    var isFishRod: Boolean = false,
    var status: Boolean = false,
    var rodLevel: Int = 0,
    var defaultFishPond: String = ""
)
