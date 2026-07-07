package cn.chahuyun.economy.game

import cn.chahuyun.economy.model.fish.FishDto
import cn.chahuyun.economy.model.fish.FishInfoDto
import cn.chahuyun.economy.model.fish.FishRankingDto
import cn.chahuyun.economy.model.user.UserInfoDto

interface GameOverviewProvider {
    fun fishingCooldownText(userInfo: UserInfoDto, isFishingTitle: Boolean, fishInfo: FishInfoDto): String
    fun farmStatusText(qq: Long): String
    fun topFishRankingWinner(): FishRankingDto?
    fun levelFishList(fishLevel: Int): List<FishDto>
}

object GameOverviewBridge {
    @Volatile
    private var provider: GameOverviewProvider? = null

    fun register(provider: GameOverviewProvider) {
        this.provider = provider
    }

    fun fishingCooldownText(userInfo: UserInfoDto, isFishingTitle: Boolean, fishInfo: FishInfoDto): String {
        return provider?.fishingCooldownText(userInfo, isFishingTitle, fishInfo) ?: "可钓鱼"
    }

    fun farmStatusText(qq: Long): String {
        return provider?.farmStatusText(qq) ?: "未种植"
    }

    fun topFishRankingWinner(): FishRankingDto? {
        return provider?.topFishRankingWinner()
    }

    fun levelFishList(fishLevel: Int): List<FishDto> {
        return provider?.levelFishList(fishLevel).orEmpty()
    }
}
