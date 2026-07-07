package cn.chahuyun.economy.service

import cn.chahuyun.economy.game.GameOverviewBridge
import cn.chahuyun.economy.model.fish.FishDto
import cn.chahuyun.economy.model.fish.FishPondDto

object FishPondQueryService {

    fun pondMoney(pond: FishPondDto): Double =
        EconomyAccountService.pluginBankBalance(pond.code, pond.description)

    fun levelFishList(pond: FishPondDto, level: Int): List<FishDto> =
        if (pond.pondType == 1) GameOverviewBridge.levelFishList(level) else emptyList()
}
