package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.farm.FarmInventory

object FarmInventoryRepository {

    @JvmStatic
    fun listInventory(qq: Long): List<FarmInventory> =
        HibernateDataStore.selectList(FarmInventory::class.java, "qq", qq)

    @JvmStatic
    fun findInventory(qq: Long, itemType: String, itemCode: String): FarmInventory? =
        listInventory(qq).firstOrNull { it.itemType == itemType && it.itemCode == itemCode }

    @JvmStatic
    fun saveInventory(inventory: FarmInventory): FarmInventory =
        HibernateDataStore.merge(inventory)

    @JvmStatic
    fun deleteInventory(inventory: FarmInventory) {
        HibernateDataStore.delete(inventory)
    }
}
