package cn.chahuyun.economy.model.farm

import cn.chahuyun.economy.constant.FarmConstants

data class FarmViewState(
    val level: Int,
    val shieldUntil: Long,
    val todaySocialCount: Int = 0,
    val dailySocialLimit: Int = 0,
    val lastSocialDate: String = "",
    val plots: List<FarmPlotView>,
    val inventory: List<FarmInventoryView>,
    val availableCrops: List<FarmCropView>,
) {
    fun shieldRemaining(now: Long): Long =
        (shieldUntil - now).coerceAtLeast(0)

    fun hasLevel(requiredLevel: Int): Boolean =
        level >= requiredLevel

    fun readyPlotNumbers(now: Long): List<Int> =
        plots.filter { it.isPlanted && it.nextMatureAt <= now }.map { it.plotNo }

    fun emptyPlotNumbers(): List<Int> =
        plots.filter { it.isEmpty }.map { it.plotNo }
}

data class FarmPlotView(
    val plotNo: Int,
    val status: String,
    val cropCode: String,
    val cropName: String = "",
    val cropEmoji: String = "",
    val currentSeason: Int = 0,
    val totalSeasons: Int = 0,
    val nextMatureAt: Long,
    val stolenSeason: Int = 0,
    val stolenAmount: Int = 0,
) {
    val isLocked: Boolean
        get() = status == FarmConstants.PLOT_LOCKED

    val isEmpty: Boolean
        get() = status == FarmConstants.PLOT_EMPTY

    val isPlanted: Boolean
        get() = status == FarmConstants.PLOT_PLANTED

    val isCurrentSeasonStolen: Boolean
        get() = currentSeason > 0 && stolenSeason == currentSeason && stolenAmount > 0
}

data class FarmInventoryView(
    val itemType: String,
    val itemCode: String,
    val displayName: String,
    val emoji: String,
    val amount: Int,
)
