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
            plots = plots.map {
                FarmPlotView(
                    plotNo = it.plotNo,
                    status = it.status,
                    cropCode = it.cropCode,
                    nextMatureAt = it.nextMatureAt,
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
