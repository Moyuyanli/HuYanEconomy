package cn.chahuyun.economy.service

import cn.chahuyun.economy.constant.FarmConstants
import cn.chahuyun.economy.constant.PropsKind
import cn.chahuyun.economy.entity.farm.FarmCrop
import cn.chahuyun.economy.entity.farm.FarmPlayer
import cn.chahuyun.economy.model.farm.FarmOperationResult
import cn.chahuyun.economy.model.farm.FarmState
import cn.chahuyun.economy.model.props.FunctionProps
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.hutool.core.util.RandomUtil
import kotlin.math.roundToLong

object FarmWaterService {
    /** 一分钟的毫秒数，用于成熟时间换算。 */
    private const val MINUTE = 60_000L

    fun dailyWaterLimit(level: Int): Int =
        FarmSocialQuotaService.dailyLimit(level)

    fun todayWaterCount(player: FarmPlayer): Int =
        FarmSocialQuotaService.todayCount(player)

    fun water(waterer: UserInfoDto, watererState: FarmState, targetState: FarmState, times: Int = 1): FarmOperationResult {
        if (waterer.qq == targetState.player.qq) {
            return FarmOperationResult(false, "不能给自己浇水")
        }

        val watererPlayer = watererState.player
        if (watererPlayer.level < FarmSocialQuotaService.REQUIRED_LEVEL) {
            return FarmOperationResult(false, "13级开放帮浇水")
        }

        FarmSocialQuotaService.refresh(watererPlayer)
        if (FarmSocialQuotaService.remaining(watererPlayer) == 0) {
            return FarmOperationResult(false, "今日农场互动次数已用完")
        }

        val requestedTimes = times.coerceAtLeast(1)
        val availableTimes = FarmSocialQuotaService.remaining(watererPlayer).coerceAtMost(requestedTimes)
        val now = System.currentTimeMillis()
        var successTimes = 0
        var totalReduceMillis = 0L
        val reducedCrops = linkedMapOf<String, Long>()
        val rewards = linkedMapOf<String, WaterReward>()

        repeat(availableTimes) {
            val plot = targetState.plots
                .filter { it.status == FarmConstants.PLOT_PLANTED && it.nextMatureAt > now }
                .maxByOrNull { it.nextMatureAt - now }
                ?: return@repeat
            val crop = FarmCropService.getCrop(plot.cropCode) ?: return FarmOperationResult(false, "目标作物数据异常")

            val reduceMillis = calculateReduceMillis(crop, plot.nextMatureAt - now)
            plot.nextMatureAt = (plot.nextMatureAt - reduceMillis).coerceAtLeast(now)
            FarmPlotService.savePlot(plot)

            successTimes += 1
            totalReduceMillis += reduceMillis
            reducedCrops[crop.name] = (reducedCrops[crop.name] ?: 0L) + reduceMillis
            rollWaterReward(crop)?.let { reward ->
                val current = rewards[reward.code]
                rewards[reward.code] = reward.copy(amount = (current?.amount ?: 0) + reward.amount)
            }
        }

        if (successTimes == 0) {
            return FarmOperationResult(false, "对方没有需要浇水的作物")
        }

        check(FarmSocialQuotaService.consume(watererPlayer, successTimes)) {
            "validated farm social quota could not be consumed"
        }
        FarmPlayerService.savePlayer(watererPlayer)
        rewards.values.forEach { reward ->
            EconomyInventoryService.addStackableProp(waterer, reward.code, PropsKind.functionProp, reward.amount)
        }

        return FarmOperationResult(true, formatWaterMessage(successTimes, requestedTimes, totalReduceMillis, reducedCrops, rewards.values))
    }

    private fun calculateReduceMillis(crop: FarmCrop, remainingMillis: Long): Long {
        val reduceMillis = if (crop.level < 10) {
            RandomUtil.randomInt(5, 16) * MINUTE
        } else {
            val fixed = RandomUtil.randomInt(20, 41) * MINUTE
            val percent = RandomUtil.randomInt(1, 6)
            fixed + (remainingMillis * percent / 100.0).roundToLong()
        }
        return reduceMillis.coerceAtMost(remainingMillis)
    }

    private fun rollWaterReward(crop: FarmCrop): WaterReward? {
        val roll = RandomUtil.randomInt(1, 101)
        val reward = when {
            crop.level < 10 && roll <= 1 -> FunctionProps.FARM_RAFFLE_ADVANCED to 1
            crop.level < 10 && roll <= 6 -> FunctionProps.FARM_RAFFLE_BASIC to 1
            crop.level >= 10 && roll <= 1 -> FunctionProps.FARM_RAFFLE_ADVANCED to 2
            crop.level >= 10 && roll <= 6 -> FunctionProps.FARM_RAFFLE_ADVANCED to 1
            else -> null
        } ?: return null

        val name = if (reward.first == FunctionProps.FARM_RAFFLE_BASIC) "初级农场抽奖券" else "高级农场抽奖券"
        return WaterReward(reward.first, name, reward.second)
    }

    private fun formatWaterMessage(
        successTimes: Int,
        requestedTimes: Int,
        totalReduceMillis: Long,
        reducedCrops: Map<String, Long>,
        rewards: Collection<WaterReward>,
    ): String {
        val totalMinutes = (totalReduceMillis / MINUTE).coerceAtLeast(1)
        val cropText = reducedCrops.entries.joinToString("、") { (name, millis) ->
            "${name}${(millis / MINUTE).coerceAtLeast(1)}分钟"
        }
        val rewardText = rewards
            .takeIf { it.isNotEmpty() }
            ?.joinToString("，", prefix = "，获得") { reward -> "${reward.name} x${reward.amount}" }
            .orEmpty()
        val partialText = if (successTimes < requestedTimes) "，剩余次数或可浇作物不足，实际执行${successTimes}次" else ""
        return if (successTimes == 1) {
            val first = reducedCrops.entries.first()
            "浇水成功，${first.key} 缩短${(first.value / MINUTE).coerceAtLeast(1)}分钟成熟时间$rewardText"
        } else {
            "批量浇水成功${successTimes}次，共缩短${totalMinutes}分钟成熟时间（$cropText）$rewardText$partialText"
        }
    }

    private data class WaterReward(
        val code: String,
        val name: String,
        val amount: Int,
    )
}
