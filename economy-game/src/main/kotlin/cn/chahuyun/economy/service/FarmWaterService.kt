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
import java.time.LocalDate
import kotlin.math.roundToLong

object FarmWaterService {
    private const val MINUTE = 60_000L
    private const val WATER_REQUIRED_LEVEL = 13
    private const val ADVANCED_WATER_LEVEL = 18
    private const val DEFAULT_DAILY_WATER_LIMIT = 5
    private const val ADVANCED_DAILY_WATER_LIMIT = 10

    fun water(waterer: UserInfoDto, watererState: FarmState, targetState: FarmState): FarmOperationResult {
        if (waterer.qq == targetState.player.qq) {
            return FarmOperationResult(false, "不能给自己浇水")
        }

        val watererPlayer = watererState.player
        if (watererPlayer.level < WATER_REQUIRED_LEVEL) {
            return FarmOperationResult(false, "13级开放帮浇水")
        }

        refreshWaterCounter(watererPlayer)
        val maxWater = if (watererPlayer.level >= ADVANCED_WATER_LEVEL) {
            ADVANCED_DAILY_WATER_LIMIT
        } else {
            DEFAULT_DAILY_WATER_LIMIT
        }
        if (watererPlayer.todayWaterCount >= maxWater) {
            return FarmOperationResult(false, "今日浇水次数已用完")
        }

        val now = System.currentTimeMillis()
        val plot = targetState.plots
            .filter { it.status == FarmConstants.PLOT_PLANTED && it.nextMatureAt > now }
            .maxByOrNull { it.nextMatureAt - now }
            ?: return FarmOperationResult(false, "对方没有需要浇水的作物")
        val crop = FarmCropService.getCrop(plot.cropCode)
            ?: return FarmOperationResult(false, "目标作物数据异常")

        val reduceMillis = calculateReduceMillis(crop, plot.nextMatureAt - now)
        plot.nextMatureAt = (plot.nextMatureAt - reduceMillis).coerceAtLeast(now)
        FarmPlotService.savePlot(plot)

        watererPlayer.todayWaterCount += 1
        FarmPlayerService.savePlayer(watererPlayer)

        val dropMessage = dropWaterReward(waterer, crop)
        val reduceMinutes = (reduceMillis / MINUTE).coerceAtLeast(1)
        return FarmOperationResult(true, "浇水成功，${crop.name} 缩短${reduceMinutes}分钟成熟时间$dropMessage")
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

    private fun refreshWaterCounter(player: FarmPlayer) {
        val today = LocalDate.now().toString()
        if (player.lastWaterDate != today) {
            player.lastWaterDate = today
            player.todayWaterCount = 0
        }
    }

    private fun dropWaterReward(waterer: UserInfoDto, crop: FarmCrop): String {
        val roll = RandomUtil.randomInt(1, 101)
        val reward = when {
            crop.level < 10 && roll <= 1 -> FunctionProps.FARM_RAFFLE_ADVANCED to 1
            crop.level < 10 && roll <= 6 -> FunctionProps.FARM_RAFFLE_BASIC to 1
            crop.level >= 10 && roll <= 1 -> FunctionProps.FARM_RAFFLE_ADVANCED to 2
            crop.level >= 10 && roll <= 6 -> FunctionProps.FARM_RAFFLE_ADVANCED to 1
            else -> null
        } ?: return ""

        EconomyInventoryService.addStackableProp(waterer, reward.first, PropsKind.functionProp, reward.second)
        val name = if (reward.first == FunctionProps.FARM_RAFFLE_BASIC) "初级农场抽奖券" else "高级农场抽奖券"
        return "，获得${name} x${reward.second}"
    }
}
