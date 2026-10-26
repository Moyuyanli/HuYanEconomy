package cn.chahuyun.economy.games.farm

data class FarmCropView(
    val code: String,
    val level: Int,
    val name: String,
    val emoji: String,
    val seedPrice: Int,
    val fruitPrice: Int,
    val firstMatureMinutes: Int,
)
