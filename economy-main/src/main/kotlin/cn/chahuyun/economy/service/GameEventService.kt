package cn.chahuyun.economy.service

import cn.chahuyun.economy.fish.FishRollEvent
import cn.chahuyun.economy.fish.FishStartEvent
import cn.chahuyun.economy.usecase.GamesUsecase

object GameEventService {

    suspend fun handleFishStart(event: FishStartEvent) {
        GamesUsecase.fishStart(event)
    }

    fun handleFishRoll(event: FishRollEvent) {
        GamesUsecase.fishRoll(event)
    }
}
