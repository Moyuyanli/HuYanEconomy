package cn.chahuyun.economy.model.fish

import kotlinx.serialization.Serializable

/**
 * Fish ranking data transfer object.
 */
@Serializable
data class FishRankingDto(
    val id: Int = 0,
    val qq: Long = 0,
    val name: String = "",
    val dimensions: Int = 0,
    val money: Double = 0.0,
    val fishRodLevel: Int = 0,
    val date: Long = 0,
    val fishName: String = "",
    val fishPondName: String = "",
    val fishLevel: Int = 0,
    val fishPondLevel: Int = 0
)
