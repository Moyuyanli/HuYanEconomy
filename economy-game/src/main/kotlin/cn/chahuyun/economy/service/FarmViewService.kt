package cn.chahuyun.economy.service

import cn.chahuyun.economy.constant.FarmConstants
import cn.chahuyun.economy.image.model.FarmDetailCard
import cn.chahuyun.economy.image.model.FarmPlotDetailLine
import cn.chahuyun.economy.image.model.FarmPlotDetailStatus
import cn.chahuyun.economy.model.farm.FarmViewState
import java.util.concurrent.TimeUnit

object FarmViewService {

    fun getOrCreateViewState(qq: Long): FarmViewState =
        FarmViewStateService.getOrCreateViewState(qq)

    fun renderFarm(state: FarmViewState, now: Long = System.currentTimeMillis()): String =
        FarmTextRenderService.renderFarm(state, now)

    fun renderShop(state: FarmViewState): String =
        FarmTextRenderService.renderShop(state)

    fun renderWarehouse(state: FarmViewState): String =
        FarmTextRenderService.renderWarehouse(state)

    fun renderFarmLevel(state: FarmViewState): String =
        FarmTextRenderService.renderFarmLevel(state)

    fun blackMarketText(state: FarmViewState): String =
        FarmTextRenderService.blackMarketText(state)

    fun farmStatusText(state: FarmViewState, now: Long = System.currentTimeMillis()): String =
        FarmTextRenderService.farmStatusText(state, now)

    fun farmDetailCard(qq: Long, owner: String, now: Long = System.currentTimeMillis()): FarmDetailCard {
        val state = getOrCreateViewState(qq)
        val unlocked = state.plots.count { !it.isLocked }
        val planted = state.plots.count { it.isPlanted }
        val ready = state.plots.count { it.isPlanted && it.nextMatureAt <= now }
        val empty = state.plots.count { it.isEmpty }
        val socialText = if (state.dailySocialLimit <= 0) {
            "13级开放"
        } else {
            "${state.todaySocialCount}/${state.dailySocialLimit}"
        }
        val socialHint = if (state.lastSocialDate.isBlank()) {
            "浇水与偷菜共享每日次数"
        } else {
            "最近互动日期 ${state.lastSocialDate}"
        }

        return FarmDetailCard(
            owner = owner,
            level = state.level,
            unlockedPlots = unlocked,
            totalPlots = FarmConstants.MAX_PLOTS,
            plantedPlots = planted,
            readyPlots = ready,
            emptyPlots = empty,
            shieldText = state.shieldRemaining(now).takeIf { it > 0 }?.let { formatDuration(it) } ?: "未激活",
            waterText = socialText,
            waterHint = socialHint,
            plots = state.plots.sortedBy { it.plotNo }.map { plot ->
                when {
                    plot.isLocked -> FarmPlotDetailLine(
                        plotNo = plot.plotNo,
                        title = "未开拓",
                        subtitle = "升级农场解锁",
                        statusText = "锁定",
                        progressText = "待开拓",
                        status = FarmPlotDetailStatus.LOCKED,
                    )

                    plot.isEmpty -> FarmPlotDetailLine(
                        plotNo = plot.plotNo,
                        title = "空闲土地",
                        subtitle = "可播种",
                        statusText = "空闲",
                        progressText = "等待种植",
                        status = FarmPlotDetailStatus.EMPTY,
                    )

                    plot.nextMatureAt <= now -> FarmPlotDetailLine(
                        plotNo = plot.plotNo,
                        title = plot.cropTitle(),
                        subtitle = seasonText(plot.currentSeason, plot.totalSeasons),
                        statusText = "可收获",
                        progressText = if (plot.isCurrentSeasonStolen) "已被偷 ${plot.stolenAmount} 个" else "已成熟",
                        status = FarmPlotDetailStatus.READY,
                    )

                    else -> FarmPlotDetailLine(
                        plotNo = plot.plotNo,
                        title = plot.cropTitle(),
                        subtitle = seasonText(plot.currentSeason, plot.totalSeasons),
                        statusText = "成长中",
                        progressText = formatDuration(plot.nextMatureAt - now),
                        status = FarmPlotDetailStatus.GROWING,
                    )
                }
            },
        )
    }

    private fun cn.chahuyun.economy.model.farm.FarmPlotView.cropTitle(): String =
        cropName.ifBlank { cropCode.ifBlank { "未知作物" } }

    private fun seasonText(currentSeason: Int, totalSeasons: Int): String =
        if (totalSeasons > 0 && currentSeason > 0) "第${currentSeason}/${totalSeasons}季" else "作物数据异常"

    private fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return if (hours > 0) "${hours}小时${minutes}分" else "${minutes.coerceAtLeast(1)}分"
    }
}
