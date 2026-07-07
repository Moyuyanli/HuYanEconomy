package cn.chahuyun.economy.service

import cn.chahuyun.economy.model.farm.FarmState

object FarmStateService {

    fun getOrCreateFarm(qq: Long): FarmState {
        val player = FarmPlayerService.findOrCreatePlayer(qq)
        val plots = FarmPlotService.ensurePlots(player)
        return FarmState(player, plots, FarmInventoryStorageService.listInventory(qq))
    }
}
