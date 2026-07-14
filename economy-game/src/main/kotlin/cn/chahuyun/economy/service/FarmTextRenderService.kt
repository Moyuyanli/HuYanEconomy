package cn.chahuyun.economy.service

import cn.chahuyun.economy.constant.FarmConstants
import cn.chahuyun.economy.model.farm.FarmPlotView
import cn.chahuyun.economy.model.farm.FarmViewState
import cn.chahuyun.economy.utils.MoneyFormatUtil
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
                "${it.emoji} ${it.name} 种子:${MoneyFormatUtil.format(it.seedPrice.toDouble())}金币 成熟:${it.firstMatureMinutes}分 售价:${MoneyFormatUtil.format(it.fruitPrice.toDouble())}"
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

    fun renderFarmLevel(state: FarmViewState): String {
        val features = listOf(
            FarmFeature(1, "基础农场", "查看农场、商店、仓库、购买种子、播种、收获、卖出果实"),
            FarmFeature(13, "农场互动", "帮好友浇水或偷取成熟作物，共享每日次数"),
            FarmFeature(14, "一键卖出", "卖出仓库内全部果实"),
            FarmFeature(15, "一键收获", "收获所有已成熟土地"),
            FarmFeature(16, "一键播种", "为空闲土地批量播种"),
            FarmFeature(17, "激活守护", "消耗当前等级升级成本的5%，开启12小时守护"),
            FarmFeature(18, "高级农场互动", "每日浇水与偷菜共享次数提升至10次"),
            FarmFeature(18, "黑市", "特殊农场功能入口"),
        )
        val unlocked = features.filter { state.hasLevel(it.level) }
        val locked = features.filterNot { state.hasLevel(it.level) }
        val next = locked.minByOrNull { it.level }

        return buildString {
            append("农场等级: Lv.${state.level}\n")
            append("地块: ${state.plots.count { !it.isLocked }}/${FarmConstants.MAX_PLOTS}\n")
            if (state.dailySocialLimit > 0) {
                append("今日农场互动: ${state.todaySocialCount}/${state.dailySocialLimit}\n")
            }
            if (next != null) {
                append("下个解锁: Lv.${next.level} ${next.name}，还差${next.level - state.level}级\n")
            } else {
                append("全部等级功能已解锁\n")
            }
            append("\n已解锁功能:\n")
            append(unlocked.joinToString("\n") { "Lv.${it.level} ${it.name}: ${it.description}" })
            if (locked.isNotEmpty()) {
                append("\n\n未解锁功能:\n")
                append(locked.joinToString("\n") { "Lv.${it.level} ${it.name}: 还差${it.level - state.level}级，${it.description}" })
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

    private data class FarmFeature(
        val level: Int,
        val name: String,
        val description: String,
    )
}
