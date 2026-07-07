package cn.chahuyun.economy.game

import cn.chahuyun.economy.model.fish.FishDto
import cn.chahuyun.economy.model.fish.FishInfoDto
import cn.chahuyun.economy.model.fish.FishRankingDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.service.GameOverviewService

object DefaultGameOverviewProvider : GameOverviewProvider {
    override fun fishingCooldownText(userInfo: UserInfoDto, isFishingTitle: Boolean, fishInfo: FishInfoDto): String {
        return GameOverviewService.fishingCooldownText(userInfo, isFishingTitle, fishInfo)
    }

    override fun farmStatusText(qq: Long): String {
        return GameOverviewService.farmStatusText(qq)
    }

    override fun topFishRankingWinner(): FishRankingDto? {
        return GameOverviewService.topFishRankingWinner()
    }

    override fun levelFishList(fishLevel: Int): List<FishDto> {
        return GameOverviewService.levelFishList(fishLevel)
    }
}
