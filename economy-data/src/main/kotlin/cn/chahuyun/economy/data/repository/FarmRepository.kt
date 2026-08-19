package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.v1.farm.FarmCrop
import cn.chahuyun.economy.entity.v1.farm.FarmInventory
import cn.chahuyun.economy.entity.v1.farm.FarmPlayer
import cn.chahuyun.economy.entity.v1.farm.FarmPlot

object FarmRepository {

    @JvmStatic
    fun findPlayer(qq: Long): FarmPlayer? =
        FarmPlayerRepository.findPlayer(qq)

    @JvmStatic
    fun savePlayer(player: FarmPlayer): FarmPlayer =
        FarmPlayerRepository.savePlayer(player)

    @JvmStatic
    fun listPlots(qq: Long): List<FarmPlot> =
        FarmPlotRepository.listPlots(qq)

    @JvmStatic
    fun savePlot(plot: FarmPlot): FarmPlot =
        FarmPlotRepository.savePlot(plot)

    @JvmStatic
    fun savePlots(plots: List<FarmPlot>): List<FarmPlot> =
        FarmPlotRepository.savePlots(plots)

    @JvmStatic
    fun listInventory(qq: Long): List<FarmInventory> =
        FarmInventoryRepository.listInventory(qq)

    @JvmStatic
    fun findInventory(qq: Long, itemType: String, itemCode: String): FarmInventory? =
        FarmInventoryRepository.findInventory(qq, itemType, itemCode)

    @JvmStatic
    fun saveInventory(inventory: FarmInventory): FarmInventory =
        FarmInventoryRepository.saveInventory(inventory)

    @JvmStatic
    fun deleteInventory(inventory: FarmInventory) {
        FarmInventoryRepository.deleteInventory(inventory)
    }

    @JvmStatic
    fun listCrops(): List<FarmCrop> =
        FarmCropRepository.listCrops()

    @JvmStatic
    fun saveCrop(crop: FarmCrop): FarmCrop =
        FarmCropRepository.saveCrop(crop)

    @JvmStatic
    fun saveCrops(crops: List<FarmCrop>): List<FarmCrop> =
        FarmCropRepository.saveCrops(crops)
}

