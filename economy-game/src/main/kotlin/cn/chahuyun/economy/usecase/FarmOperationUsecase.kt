package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.manager.FarmManager
import cn.chahuyun.economy.service.EconomyUserService
import cn.chahuyun.economy.service.FarmCommandService
import net.mamoe.mirai.event.events.GroupMessageEvent

object FarmOperationUsecase {

    suspend fun buySeed(event: GroupMessageEvent) {
        val raw = FarmUsecaseSupport.commandPayload(event, "购买种子")
        val result = FarmCommandService.buySeed(event.sender, raw)
        FarmUsecaseSupport.reply(event, result.message)
    }

    suspend fun plant(event: GroupMessageEvent) {
        val raw = FarmUsecaseSupport.commandPayload(event, listOf("播种", "种植"))
        val result = FarmCommandService.plant(event.sender, raw)
        FarmUsecaseSupport.reply(event, result.message)
    }

    suspend fun harvest(event: GroupMessageEvent) {
        val raw = FarmUsecaseSupport.commandPayload(event, "收获")
        val result = FarmCommandService.harvest(event.sender.id, raw)
        FarmUsecaseSupport.reply(event, result.message)
    }

    suspend fun sellFruits(event: GroupMessageEvent) {
        val raw = FarmUsecaseSupport.commandPayload(event, "卖出果实")
        val result = FarmCommandService.sellFruits(event.sender, raw)
        FarmUsecaseSupport.reply(event, result.message)
    }

    suspend fun upgradeFarm(event: GroupMessageEvent) {
        val result = FarmManager.upgradeFarm(event.sender)
        FarmUsecaseSupport.reply(event, result.message)
    }

    suspend fun water(event: GroupMessageEvent) {
        val target = FarmUsecaseSupport.atTargetOrReply(event) ?: return
        val userInfo = EconomyUserService.getOrCreate(event.sender)
        val result = FarmManager.water(userInfo, target)
        FarmUsecaseSupport.reply(event, result.message)
    }

    suspend fun sellAll(event: GroupMessageEvent) {
        val result = FarmCommandService.sellAll(event.sender)
        FarmUsecaseSupport.reply(event, result.message)
    }

    suspend fun harvestAll(event: GroupMessageEvent) {
        val result = FarmCommandService.harvestAll(event.sender.id)
        FarmUsecaseSupport.reply(event, result.message)
    }

    suspend fun plantAll(event: GroupMessageEvent) {
        val cropName = FarmUsecaseSupport.commandPayload(event, "一键播种")
        val result = FarmCommandService.plantAll(event.sender, cropName)
        FarmUsecaseSupport.reply(event, result.message)
    }

    suspend fun activateShield(event: GroupMessageEvent) {
        val result = FarmManager.activateShield(event.sender.id)
        FarmUsecaseSupport.reply(event, result.message)
    }
}
