package cn.chahuyun.economy.service

import cn.chahuyun.economy.constant.FarmConstants
import cn.chahuyun.economy.data.repository.FarmStealRepository
import cn.chahuyun.economy.entity.farm.FarmCrop
import cn.chahuyun.economy.entity.farm.FarmPlot
import cn.chahuyun.economy.model.farm.FarmOperationResult
import cn.chahuyun.economy.model.farm.FarmState
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.hutool.core.util.RandomUtil

object FarmStealService {

    fun steal(
        thief: UserInfoDto,
        thiefState: FarmState,
        targetState: FarmState,
        now: Long = System.currentTimeMillis(),
    ): FarmOperationResult {
        if (thief.qq == targetState.player.qq) {
            return FarmOperationResult(false, "不能偷自己的菜")
        }

        val thiefPlayer = thiefState.player
        if (thiefPlayer.level < FarmSocialQuotaService.REQUIRED_LEVEL) {
            return FarmOperationResult(false, "13级开放偷菜")
        }

        FarmSocialQuotaService.refresh(thiefPlayer)
        if (FarmSocialQuotaService.remaining(thiefPlayer) == 0) {
            return FarmOperationResult(false, "今日农场互动次数已用完")
        }

        val candidates = targetState.plots.mapNotNull { plot ->
            if (plot.status != FarmConstants.PLOT_PLANTED) return@mapNotNull null
            val crop = FarmCropService.getCrop(plot.cropCode) ?: return@mapNotNull null
            if (FarmStealRules.isStealable(plot, crop, now)) StealCandidate(plot, crop) else null
        }
        if (candidates.isEmpty()) {
            return FarmOperationResult(false, "对方没有可以偷取的成熟作物")
        }

        val candidate = candidates[RandomUtil.randomInt(candidates.size)]
        check(FarmSocialQuotaService.consume(thiefPlayer)) { "validated farm social quota could not be consumed" }
        FarmPlayerService.savePlayer(thiefPlayer)
        val quotaText = "今日农场互动 ${thiefPlayer.todayWaterCount}/${FarmSocialQuotaService.dailyLimit(thiefPlayer.level)}"

        if (targetState.player.shieldUntil > now) {
            return FarmOperationResult(false, "对方农场守护中，偷菜失败，$quotaText")
        }

        val successRate = FarmStealRules.successRate(thiefPlayer.level, targetState.player.level)
        val roll = RandomUtil.randomInt(1, 101)
        if (roll > successRate) {
            return FarmOperationResult(false, "偷菜失败，本次成功率${successRate}%，$quotaText")
        }

        val amount = FarmStealRules.stealAmount(
            candidate.crop.yieldPerSeason,
            thiefPlayer.level,
            targetState.player.level,
        ) { maxAmount -> RandomUtil.randomInt(1, maxAmount + 1) }
        val recorded = FarmStealRepository.recordSuccessfulSteal(
            plotId = candidate.plot.id,
            expectedSeason = candidate.plot.currentSeason,
            expectedCropCode = candidate.crop.code,
            thiefQq = thief.qq,
            fruitItemType = FarmConstants.ITEM_FRUIT,
            amount = amount,
        )
        if (!recorded) {
            return FarmOperationResult(false, "来晚一步，这季作物已经被偷过了，$quotaText")
        }

        candidate.plot.stolenSeason = candidate.plot.currentSeason
        candidate.plot.stolenAmount = amount
        return FarmOperationResult(
            true,
            "偷菜成功，从${candidate.plot.plotNo}号地获得${candidate.crop.name} x$amount，$quotaText",
        )
    }

    private data class StealCandidate(
        val plot: FarmPlot,
        val crop: FarmCrop,
    )
}
