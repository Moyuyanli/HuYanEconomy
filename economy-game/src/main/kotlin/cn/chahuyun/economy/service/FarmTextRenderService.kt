package cn.chahuyun.economy.service

import cn.chahuyun.economy.constant.FarmConstants
import cn.chahuyun.economy.model.farm.FarmPlotView
import cn.chahuyun.economy.model.farm.FarmViewState
import java.util.concurrent.TimeUnit

object FarmTextRenderService {

    fun renderFarm(state: FarmViewState, now: Long = System.currentTimeMillis()): String =
        buildString {
            append("农场等级:${state.level}\n")
            val shield = state.shieldRemaining(now)
            if (shield > 0) append("守护剩余:${formatDuration(shield)}\n")
            append("土地:\n")
            append(renderPlots(state.plots, now))
        }

    fun renderShop(state: FarmViewState): String {
        val crops = state.availableCrops
        return if (crops.isEmpty()) {
            "农场商店暂无可购买种子"
        } else {
            crops.joinToString("\n", "农场商店(${state.level}级可购买):\n") {
                "${it.emoji} ${it.name} 种子:${it.seedPrice}金币 成熟:${it.firstMatureMinutes}分 售价:${it.fruitPrice}"
            }
        }
    }

    fun renderWarehouse(state: FarmViewState): String =
        if (state.inventory.isEmpty()) {
            "农场仓库是空的"
        } else {
            state.inventory.groupBy { it.itemType }.entries.joinToString("\n") { (type, list) ->
                val title = if (type == FarmConstants.ITEM_SEED) "种子" else "果实"
                list.joinToString("\n", "$title:\n") { inventory ->
                    "${inventory.emoji} ${inventory.displayName} x${inventory.amount}"
                }
            }
        }

    fun blackMarketText(state: FarmViewState): String =
        if (state.hasLevel(18)) "黑市尚未开放" else "18级开放黑市"

    fun farmStatusText(state: FarmViewState, now: Long = System.currentTimeMillis()): String {
        val plots = state.plots.filterNot { it.isLocked }
        val planted = plots.filter { it.isPlanted }
        if (planted.isEmpty()) return "未种植"
        val mature = planted.count { it.nextMatureAt <= now }
        return when {
            mature == 0 -> "成长中"
            mature == planted.size -> "全成熟"
            else -> "有成熟"
        }
    }

    private fun renderPlots(plots: List<FarmPlotView>, now: Long): String {
        return plots.chunked(6).joinToString("\n") { row ->
            row.joinToString(" ") { plot ->
                val status = when (plot.status) {
                    FarmConstants.PLOT_LOCKED -> "锁"
                    FarmConstants.PLOT_EMPTY -> "空"
                    else -> if (plot.nextMatureAt <= now) "熟" else "苗"
                }
                "%02d:%s".format(plot.plotNo, status)
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return "${hours}小时${minutes}分钟"
    }
}
