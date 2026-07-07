package cn.chahuyun.economy.service

import cn.chahuyun.economy.model.fish.FishInfoDto
import cn.chahuyun.economy.model.fish.level
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.model.user.user
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.SingleMessage

object FishingRodUpgradeService {

    fun upgrade(fishInfo: FishInfoDto, userInfo: UserInfoDto): SingleMessage {
        val user = userInfo.user
        val moneyByUser = EconomyAccountService.walletBalance(user)
        val upMoney = FishingRodUpgradeRules.upgradeCost(fishInfo.rodLevel, fishInfo.level)
            ?: return FishingRodMessageFormatter.maxLevel()
        return upgradeIfAffordable(fishInfo, user, moneyByUser, upMoney)
    }

    private fun upFishRod(fishInfo: FishInfoDto) {
        fishInfo.rodLevel += 1
        FishRuntimeDataService.saveFishInfo(fishInfo)
    }

    private fun upgradeIfAffordable(
        fishInfo: FishInfoDto,
        user: User,
        userMoney: Double,
        upMoney: Int,
    ): SingleMessage {
        if (userMoney - upMoney < 0) {
            return FishingRodMessageFormatter.coinNotEnough(upMoney)
        }
        if (EconomyAccountService.subtractWallet(user, upMoney.toDouble())) {
            val oldLevel = fishInfo.rodLevel
            upFishRod(fishInfo)
            return FishingRodMessageFormatter.upgradeSuccess(upMoney, oldLevel, fishInfo.rodLevel)
        }
        return FishingRodMessageFormatter.upgradeFailed()
    }
}
