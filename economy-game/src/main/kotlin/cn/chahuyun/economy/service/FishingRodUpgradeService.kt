package cn.chahuyun.economy.service

import cn.chahuyun.economy.model.fish.FishInfoDto
import cn.chahuyun.economy.model.fish.level
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.model.user.user
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.SingleMessage

object FishingRodUpgradeService {

    fun upgrade(fishInfo: FishInfoDto, userInfo: UserInfoDto): SingleMessage {
        return PlainText(upgradeResult(fishInfo, userInfo).message)
    }

    fun upgradeUntilFailure(fishInfo: FishInfoDto, userInfo: UserInfoDto): String =
        UpgradeLoopService.runUntilFailure {
            upgradeResult(fishInfo, userInfo)
        }

    private fun upgradeResult(fishInfo: FishInfoDto, userInfo: UserInfoDto): UpgradeStepResult {
        val user = userInfo.user
        val moneyByUser = EconomyAccountService.walletBalance(user)
        val upMoney = FishingRodUpgradeRules.upgradeCost(fishInfo.rodLevel, fishInfo.level)
            ?: return UpgradeStepResult(false, FishingRodMessageFormatter.maxLevel().contentToString())
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
    ): UpgradeStepResult {
        if (userMoney - upMoney < 0) {
            return UpgradeStepResult(false, FishingRodMessageFormatter.coinNotEnough(upMoney).contentToString())
        }
        if (EconomyAccountService.subtractWallet(user, upMoney.toDouble())) {
            val oldLevel = fishInfo.rodLevel
            upFishRod(fishInfo)
            return UpgradeStepResult(
                true,
                FishingRodMessageFormatter.upgradeSuccess(upMoney, oldLevel, fishInfo.rodLevel).contentToString(),
            )
        }
        return UpgradeStepResult(false, FishingRodMessageFormatter.upgradeFailed().contentToString())
    }
}
