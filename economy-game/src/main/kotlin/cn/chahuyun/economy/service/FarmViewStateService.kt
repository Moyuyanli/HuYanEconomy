package cn.chahuyun.economy.service

import cn.chahuyun.economy.model.farm.FarmInventoryView
import cn.chahuyun.economy.model.farm.FarmPlotView
import cn.chahuyun.economy.model.farm.FarmState
import cn.chahuyun.economy.model.farm.FarmViewState

object FarmViewStateService {

    fun getOrCreateViewState(qq: Long): FarmViewState =
        FarmStateService.getOrCreateFarm(qq).toViewState()

    fun FarmState.toViewState(): FarmViewState =
        FarmViewState(
            level = player.level,
            shieldUntil = player.shieldUntil,
            todaySocialCount = FarmSocialQuotaService.todayCount(player),
            dailySocialLimit = FarmSocialQuotaService.dailyLimit(player.level),
            lastSocialDate = player.lastWaterDate,
            plots = plots.map {
                val crop = FarmCropService.getCrop(it.cropCode)
                FarmPlotView(
                    plotNo = it.plotNo,
                    status = it.status,
                    cropCode = it.cropCode,
                    cropName = crop?.name.orEmpty(),
                    cropEmoji = crop?.emoji.orEmpty(),
                    currentSeason = it.currentSeason,
                    totalSeasons = crop?.totalSeasons ?: 0,
                    nextMatureAt = it.nextMatureAt,
                    stolenSeason = it.stolenSeason,
                    stolenAmount = it.stolenAmount,
                )
            },
            inventory = inventory.map {
                val crop = FarmCropService.getCropView(it.itemCode)
                FarmInventoryView(
                    itemType = it.itemType,
                    itemCode = it.itemCode,
                    displayName = crop?.name ?: it.itemCode,
                    emoji = crop?.emoji ?: "",
                    amount = it.amount,
                )
            },
            availableCrops = FarmCropService.listCropViewsForLevel(player.level),
        )
}
