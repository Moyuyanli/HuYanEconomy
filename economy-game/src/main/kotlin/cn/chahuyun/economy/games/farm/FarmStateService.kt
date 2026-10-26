package cn.chahuyun.economy.games.farm


object FarmStateService {

    fun getOrCreateFarm(qq: Long): FarmState {
        val player = FarmPlayerService.findOrCreatePlayer(qq)
        val plots = FarmPlotService.ensurePlots(player)
        return FarmState(player, plots, FarmInventoryStorageService.listInventory(qq))
    }
}
