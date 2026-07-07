package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.farm.FarmPlot

object FarmPlotRepository {

    @JvmStatic
    fun listPlots(qq: Long): List<FarmPlot> =
        HibernateDataStore.selectList(FarmPlot::class.java, "qq", qq).sortedBy { it.plotNo }

    @JvmStatic
    fun savePlot(plot: FarmPlot): FarmPlot =
        HibernateDataStore.merge(plot)

    @JvmStatic
    fun savePlots(plots: List<FarmPlot>): List<FarmPlot> =
        plots.map { savePlot(it) }
}
