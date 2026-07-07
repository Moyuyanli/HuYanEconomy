package cn.chahuyun.economy.service

import cn.chahuyun.economy.constant.FarmConstants
import cn.chahuyun.economy.data.repository.FarmInventoryRepository
import cn.chahuyun.economy.entity.farm.FarmInventory

object FarmInventoryStorageService {

    fun listInventory(qq: Long): List<FarmInventory> =
        FarmInventoryRepository.listInventory(qq)

    fun listFruits(qq: Long): List<FarmInventory> =
        listInventory(qq).filter { it.itemType == FarmConstants.ITEM_FRUIT }

    fun addInventory(qq: Long, itemType: String, itemCode: String, amount: Int): FarmInventory {
        require(amount > 0) { "amount must be positive" }
        val inventory = FarmInventoryRepository.findInventory(qq, itemType, itemCode) ?: FarmInventory().apply {
            this.qq = qq
            this.itemType = itemType
            this.itemCode = itemCode
        }
        inventory.amount += amount
        return FarmInventoryRepository.saveInventory(inventory)
    }

    fun removeInventory(qq: Long, itemType: String, itemCode: String, amount: Int): Boolean {
        if (amount <= 0) return false
        val inventory = FarmInventoryRepository.findInventory(qq, itemType, itemCode) ?: return false
        if (inventory.amount < amount) return false
        inventory.amount -= amount
        if (inventory.amount == 0) {
            FarmInventoryRepository.deleteInventory(inventory)
        } else {
            FarmInventoryRepository.saveInventory(inventory)
        }
        return true
    }

    fun inventoryAmount(qq: Long, itemType: String, itemCode: String): Int =
        FarmInventoryRepository.findInventory(qq, itemType, itemCode)?.amount ?: 0
}
