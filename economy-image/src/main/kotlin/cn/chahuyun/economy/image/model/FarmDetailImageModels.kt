package cn.chahuyun.economy.image.model

data class FarmDetailCard(
    val owner: String,
    val level: Int,
    val unlockedPlots: Int,
    val totalPlots: Int,
    val plantedPlots: Int,
    val readyPlots: Int,
    val emptyPlots: Int,
    val shieldText: String,
    val waterText: String,
    val waterHint: String,
    val plots: List<FarmPlotDetailLine>,
)

data class FarmPlotDetailLine(
    val plotNo: Int,
    val title: String,
    val subtitle: String,
    val statusText: String,
    val progressText: String,
    val status: FarmPlotDetailStatus,
)

enum class FarmPlotDetailStatus {
    LOCKED,
    EMPTY,
    GROWING,
    READY,
}
