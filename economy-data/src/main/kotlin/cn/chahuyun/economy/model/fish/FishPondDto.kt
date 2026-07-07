package cn.chahuyun.economy.model.fish

import kotlinx.serialization.Serializable

/**
 * Fish pond data transfer object.
 */
@Serializable
data class FishPondDto(
    var id: Int = 0,
    var code: String = "",
    var admin: Long = 0,
    var pondType: Int = 0,
    var name: String = "",
    var description: String = "",
    var pondLevel: Int = 0,
    var minLevel: Int = 0,
    var rebate: Double = 0.05,
    var number: Int = 0,
    var fishCount: Int = 0
)
