package cn.chahuyun.economy.usecase

import net.mamoe.mirai.event.events.GroupMessageEvent

object FarmUsecase {

    suspend fun viewFarm(event: GroupMessageEvent) {
        FarmViewUsecase.viewFarm(event)
    }

    suspend fun viewShop(event: GroupMessageEvent) {
        FarmViewUsecase.viewShop(event)
    }

    suspend fun viewWarehouse(event: GroupMessageEvent) {
        FarmViewUsecase.viewWarehouse(event)
    }

    suspend fun viewFarmDetail(event: GroupMessageEvent) {
        FarmViewUsecase.viewFarmDetail(event)
    }

    suspend fun buySeed(event: GroupMessageEvent) {
        FarmOperationUsecase.buySeed(event)
    }

    suspend fun plant(event: GroupMessageEvent) {
        FarmOperationUsecase.plant(event)
    }

    suspend fun harvest(event: GroupMessageEvent) {
        FarmOperationUsecase.harvest(event)
    }

    suspend fun sellFruits(event: GroupMessageEvent) {
        FarmOperationUsecase.sellFruits(event)
    }

    suspend fun upgradeFarm(event: GroupMessageEvent) {
        FarmOperationUsecase.upgradeFarm(event)
    }

    suspend fun water(event: GroupMessageEvent) {
        FarmOperationUsecase.water(event)
    }

    suspend fun sellAll(event: GroupMessageEvent) {
        FarmOperationUsecase.sellAll(event)
    }

    suspend fun harvestAll(event: GroupMessageEvent) {
        FarmOperationUsecase.harvestAll(event)
    }

    suspend fun plantAll(event: GroupMessageEvent) {
        FarmOperationUsecase.plantAll(event)
    }

    suspend fun activateShield(event: GroupMessageEvent) {
        FarmOperationUsecase.activateShield(event)
    }

    suspend fun blackMarket(event: GroupMessageEvent) {
        FarmViewUsecase.blackMarket(event)
    }

}
