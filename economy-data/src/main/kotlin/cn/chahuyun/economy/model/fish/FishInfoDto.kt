package cn.chahuyun.economy.model.fish

import kotlinx.serialization.Serializable

/**
 * Player fish info data transfer object.
 */
@Serializable
data class FishInfoDto(
    /**
     * id
     */
    var id: Long = 0,
    /**
     * qq
     */
    var qq: Long = 0,
    /**
     * 是否拥有鱼竿
     */
    var isFishRod: Boolean = false,
    /**
     * 是否正在钓鱼
     */
    var status: Boolean = false,
    /**
     * 鱼竿等级
     */
    var rodLevel: Int = 0,
    /**
     * 默认鱼塘编码
     */
    var defaultFishPond: String = ""
)
