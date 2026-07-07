package cn.chahuyun.economy.service

import cn.chahuyun.economy.data.repository.FishRepository
import cn.chahuyun.economy.model.fish.FishDto
import cn.chahuyun.economy.model.fish.FishInfoDto
import cn.chahuyun.economy.model.fish.FishRankingDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.utils.Log

object GameOverviewService {

    fun fishingCooldownText(userInfo: UserInfoDto, isFishingTitle: Boolean, fishInfo: FishInfoDto): String =
        FishingRuntimeService.getFishingCooldownText(userInfo, isFishingTitle, fishInfo)

    fun farmStatusText(qq: Long): String {
        return try {
            FarmViewService.farmStatusText(FarmViewService.getOrCreateViewState(qq))
        } catch (e: Exception) {
            Log.error("获取农场状态失败", e)
            "未种植"
        }
    }

    fun topFishRankingWinner(): FishRankingDto? =
        FishRepository.topRankingWinner()

    fun levelFishList(fishLevel: Int): List<FishDto> =
        FishCatalogService.levelFishList(fishLevel)
}
