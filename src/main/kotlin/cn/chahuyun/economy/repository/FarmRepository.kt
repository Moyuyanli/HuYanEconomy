package cn.chahuyun.economy.repository

import cn.chahuyun.economy.entity.farm.FarmCrop
import cn.chahuyun.economy.entity.farm.FarmInventory
import cn.chahuyun.economy.entity.farm.FarmPlayer
import cn.chahuyun.economy.entity.farm.FarmPlot
import cn.chahuyun.hibernateplus.HibernateFactory

object FarmRepository {

    @JvmStatic
    fun findPlayer(qq: Long): FarmPlayer? = HibernateFactory.selectOne(FarmPlayer::class.java, "qq", qq)

    @JvmStatic
    fun savePlayer(player: FarmPlayer): FarmPlayer = HibernateFactory.merge(player)

    @JvmStatic
    fun listPlots(qq: Long): List<FarmPlot> =
        HibernateFactory.selectList(FarmPlot::class.java, "qq", qq).sortedBy { it.plotNo }

    @JvmStatic
    fun savePlot(plot: FarmPlot): FarmPlot = HibernateFactory.merge(plot)

    @JvmStatic
    fun savePlots(plots: List<FarmPlot>): List<FarmPlot> = plots.map { savePlot(it) }

    @JvmStatic
    fun listInventory(qq: Long): List<FarmInventory> =
        HibernateFactory.selectList(FarmInventory::class.java, "qq", qq)

    @JvmStatic
    fun findInventory(qq: Long, itemType: String, itemCode: String): FarmInventory? =
        listInventory(qq).firstOrNull { it.itemType == itemType && it.itemCode == itemCode }

    @JvmStatic
    fun saveInventory(inventory: FarmInventory): FarmInventory = HibernateFactory.merge(inventory)

    @JvmStatic
    fun deleteInventory(inventory: FarmInventory) = HibernateFactory.delete(inventory)

    @JvmStatic
    fun listCrops(): List<FarmCrop> = HibernateFactory.selectList(FarmCrop::class.java).sortedBy { it.level }

    @JvmStatic
    fun saveCrop(crop: FarmCrop): FarmCrop = HibernateFactory.merge(crop)

    @JvmStatic
    fun saveCrops(crops: List<FarmCrop>): List<FarmCrop> = crops.map { saveCrop(it) }
}
