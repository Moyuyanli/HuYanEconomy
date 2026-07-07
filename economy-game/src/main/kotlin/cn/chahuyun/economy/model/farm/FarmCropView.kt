package cn.chahuyun.economy.model.farm

data class FarmCropView(
    val code: String,
    val level: Int,
    val name: String,
    val emoji: String,
    val seedPrice: Int,
    val fruitPrice: Int,
    val firstMatureMinutes: Int,
)
