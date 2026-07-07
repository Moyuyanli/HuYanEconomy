package cn.chahuyun.economy.service

import cn.chahuyun.economy.constant.FarmConstants
import cn.chahuyun.economy.data.repository.FarmPlotRepository
import cn.chahuyun.economy.entity.farm.FarmPlayer
import cn.chahuyun.economy.entity.farm.FarmPlot

object FarmPlotService {

    fun unlockedPlotCount(level: Int): Int =
        (FarmConstants.INITIAL_UNLOCKED_PLOTS + level - 1).coerceAtMost(FarmConstants.MAX_PLOTS)

    fun savePlot(plot: FarmPlot): FarmPlot =
        FarmPlotRepository.savePlot(plot)

    fun ensurePlots(player: FarmPlayer): List<FarmPlot> {
        val existing = FarmPlotRepository.listPlots(player.qq).associateBy { it.plotNo }
        val unlocked = unlockedPlotCount(player.level)
        val plots = (1..FarmConstants.MAX_PLOTS).map { plotNo ->
            val plot = existing[plotNo] ?: FarmPlot().apply {
                qq = player.qq
                this.plotNo = plotNo
            }
            if (plot.status == FarmConstants.PLOT_LOCKED && plotNo <= unlocked) {
                plot.status = FarmConstants.PLOT_EMPTY
            }
            if (plot.id == 0L) {
                plot.status = if (plotNo <= unlocked) FarmConstants.PLOT_EMPTY else FarmConstants.PLOT_LOCKED
            }
            savePlot(plot)
        }
        return plots.sortedBy { it.plotNo }
    }
}
