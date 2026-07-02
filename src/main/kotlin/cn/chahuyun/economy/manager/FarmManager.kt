package cn.chahuyun.economy.manager

import cn.chahuyun.economy.constant.FarmConstants
import cn.chahuyun.economy.constant.PropsKind
import cn.chahuyun.economy.entity.farm.FarmCrop
import cn.chahuyun.economy.entity.farm.FarmInventory
import cn.chahuyun.economy.entity.farm.FarmPlayer
import cn.chahuyun.economy.entity.farm.FarmPlot
import cn.chahuyun.economy.model.props.FunctionProps
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.plugin.FarmCropManager
import cn.chahuyun.economy.repository.FarmRepository
import cn.chahuyun.economy.utils.EconomyUtil
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.contact.User
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.roundToLong

data class FarmState(
    val player: FarmPlayer,
    val plots: List<FarmPlot>,
    val inventory: List<FarmInventory>,
)

data class FarmOperationResult(
    val success: Boolean,
    val message: String,
)

object FarmManager {
    private const val MINUTE = 60_000L
    private val locks = ConcurrentHashMap<Long, ReentrantLock>()

    @JvmStatic
    fun init() {
        // Placeholder for future farm scheduled jobs.
    }

    fun <T> withUserLock(qq: Long, block: () -> T): T =
        locks.getOrPut(qq) { ReentrantLock() }.withLock(block)

    private fun <T> withTwoUserLocks(firstQq: Long, secondQq: Long, block: () -> T): T {
        if (firstQq == secondQq) return withUserLock(firstQq, block)
        val first = minOf(firstQq, secondQq)
        val second = maxOf(firstQq, secondQq)
        return locks.getOrPut(first) { ReentrantLock() }.withLock {
            locks.getOrPut(second) { ReentrantLock() }.withLock(block)
        }
    }

    fun getOrCreateFarm(qq: Long): FarmState = withUserLock(qq) {
        val player = FarmRepository.findPlayer(qq) ?: FarmRepository.savePlayer(FarmPlayer().apply { this.qq = qq })
        val plots = ensurePlots(player)
        FarmState(player, plots, FarmRepository.listInventory(qq))
    }

    fun unlockedPlotCount(level: Int): Int =
        (FarmConstants.INITIAL_UNLOCKED_PLOTS + level - 1).coerceAtMost(FarmConstants.MAX_PLOTS)

    fun parsePlotNumbers(text: String): List<Int> {
        val normalized = text.replace('，', ',').replace(',', ' ')
        val numbers = linkedSetOf<Int>()
        normalized.split(Regex("\\s+")).filter { it.isNotBlank() }.forEach { token ->
            val range = Regex("^(\\d+)-(\\d+)$").matchEntire(token)
            if (range != null) {
                val start = range.groupValues[1].toInt()
                val end = range.groupValues[2].toInt()
                val progression = if (start <= end) start..end else end..start
                numbers.addAll(progression)
            } else {
                token.toIntOrNull()?.let { numbers.add(it) }
            }
        }
        return numbers.filter { it in 1..FarmConstants.MAX_PLOTS }.sorted()
    }

    fun addInventory(qq: Long, itemType: String, itemCode: String, amount: Int): FarmInventory {
        require(amount > 0) { "amount must be positive" }
        val inventory = FarmRepository.findInventory(qq, itemType, itemCode) ?: FarmInventory().apply {
            this.qq = qq
            this.itemType = itemType
            this.itemCode = itemCode
        }
        inventory.amount += amount
        return FarmRepository.saveInventory(inventory)
    }

    fun removeInventory(qq: Long, itemType: String, itemCode: String, amount: Int): Boolean {
        if (amount <= 0) return false
        val inventory = FarmRepository.findInventory(qq, itemType, itemCode) ?: return false
        if (inventory.amount < amount) return false
        inventory.amount -= amount
        if (inventory.amount == 0) {
            FarmRepository.deleteInventory(inventory)
        } else {
            FarmRepository.saveInventory(inventory)
        }
        return true
    }

    fun inventoryAmount(qq: Long, itemType: String, itemCode: String): Int =
        FarmRepository.findInventory(qq, itemType, itemCode)?.amount ?: 0

    fun buySeed(user: User, crop: FarmCrop, amount: Int): FarmOperationResult = withUserLock(user.id) {
        if (amount <= 0) return@withUserLock FarmOperationResult(false, "购买数量必须大于0")
        val player = getOrCreateFarm(user.id).player
        if (player.level < crop.level) {
            return@withUserLock FarmOperationResult(false, "${crop.name} 需要农场${crop.level}级才能购买")
        }
        val cost = crop.seedPrice.toDouble() * amount
        if (EconomyUtil.getMoneyByUser(user) < cost) {
            return@withUserLock FarmOperationResult(false, "金币不足，需要${cost.toLong()}金币")
        }
        if (!EconomyUtil.minusMoneyToUser(user, cost)) {
            return@withUserLock FarmOperationResult(false, "扣款失败")
        }
        runCatching {
            addInventory(user.id, FarmConstants.ITEM_SEED, crop.code, amount)
        }.onFailure {
            EconomyUtil.plusMoneyToUser(user, cost)
            return@withUserLock FarmOperationResult(false, "种子入库失败，已退款")
        }
        FarmOperationResult(true, "购买 ${crop.name} 种子 x$amount 成功，花费${cost.toLong()}金币")
    }

    fun plant(user: User, plotNumbers: List<Int>, crop: FarmCrop): FarmOperationResult = withUserLock(user.id) {
        val state = getOrCreateFarm(user.id)
        if (state.player.level < crop.level) {
            return@withUserLock FarmOperationResult(false, "${crop.name} 需要农场${crop.level}级才能播种")
        }
        val plotsByNo = state.plots.associateBy { it.plotNo }
        val plantable = plotNumbers.mapNotNull { plotsByNo[it] }
            .filter { it.status == FarmConstants.PLOT_EMPTY }
        if (plantable.isEmpty()) {
            return@withUserLock FarmOperationResult(false, "没有可播种的空闲土地")
        }
        val seedAmount = inventoryAmount(user.id, FarmConstants.ITEM_SEED, crop.code)
        val selected = plantable.take(seedAmount)
        if (selected.isEmpty()) {
            return@withUserLock FarmOperationResult(false, "${crop.name} 种子不足")
        }
        removeInventory(user.id, FarmConstants.ITEM_SEED, crop.code, selected.size)
        val now = System.currentTimeMillis()
        selected.forEach { plot ->
            plot.status = FarmConstants.PLOT_PLANTED
            plot.cropCode = crop.code
            plot.plantedAt = now
            plot.currentSeason = 1
            plot.nextMatureAt = now + crop.firstMatureMinutes * MINUTE
            FarmRepository.savePlot(plot)
        }
        val skipped = plantable.size - selected.size
        val suffix = if (skipped > 0) "，种子不足跳过${skipped}块" else ""
        FarmOperationResult(true, "播种 ${crop.name} x${selected.size} 成功$suffix")
    }

    fun harvest(qq: Long, plotNumbers: List<Int>): FarmOperationResult = withUserLock(qq) {
        val state = getOrCreateFarm(qq)
        val plotsByNo = state.plots.associateBy { it.plotNo }
        val now = System.currentTimeMillis()
        var harvested = 0
        var fruits = 0
        val messages = mutableListOf<String>()

        plotNumbers.forEach { plotNo ->
            val plot = plotsByNo[plotNo]
            if (plot == null || plot.status != FarmConstants.PLOT_PLANTED) {
                messages += "${plotNo}号地没有可收获作物"
                return@forEach
            }
            if (now < plot.nextMatureAt) {
                messages += "${plotNo}号地还未成熟"
                return@forEach
            }
            val crop = FarmCropManager.getCrop(plot.cropCode)
            if (crop == null) {
                messages += "${plotNo}号地作物数据异常"
                return@forEach
            }
            addInventory(qq, FarmConstants.ITEM_FRUIT, crop.code, crop.yieldPerSeason)
            harvested += 1
            fruits += crop.yieldPerSeason
            if (plot.currentSeason < crop.totalSeasons && crop.nextMatureMinutes > 0) {
                plot.currentSeason += 1
                plot.nextMatureAt = now + crop.nextMatureMinutes * MINUTE
            } else {
                clearPlot(plot)
            }
            FarmRepository.savePlot(plot)
        }

        if (harvested == 0) {
            FarmOperationResult(false, messages.joinToString("\n").ifBlank { "没有收获任何作物" })
        } else {
            val detail = if (messages.isEmpty()) "" else "\n" + messages.joinToString("\n")
            FarmOperationResult(true, "收获${harvested}块土地，获得果实${fruits}个$detail")
        }
    }

    fun sellFruits(user: User, crop: FarmCrop?, amount: Int?): FarmOperationResult = withUserLock(user.id) {
        val inventories = FarmRepository.listInventory(user.id).filter { it.itemType == FarmConstants.ITEM_FRUIT }
        val targets = if (crop == null) inventories else inventories.filter { it.itemCode == crop.code }
        if (targets.isEmpty()) return@withUserLock FarmOperationResult(false, "没有可卖出的果实")

        var totalMoney = 0L
        var sold = 0
        val deductions = mutableListOf<Triple<String, Int, FarmCrop>>()
        for (inventory in targets) {
            val targetCrop = FarmCropManager.getCrop(inventory.itemCode) ?: continue
            val sellAmount = if (crop == null) inventory.amount else (amount ?: inventory.amount).coerceAtMost(inventory.amount)
            if (sellAmount <= 0) continue
            totalMoney += sellAmount.toLong() * targetCrop.fruitPrice
            sold += sellAmount
            deductions += Triple(inventory.itemCode, sellAmount, targetCrop)
            if (crop != null) break
        }
        if (sold == 0 || totalMoney <= 0) return@withUserLock FarmOperationResult(false, "没有可卖出的果实")
        deductions.forEach { removeInventory(user.id, FarmConstants.ITEM_FRUIT, it.first, it.second) }
        if (!EconomyUtil.plusMoneyToUser(user, totalMoney.toDouble())) {
            deductions.forEach { addInventory(user.id, FarmConstants.ITEM_FRUIT, it.first, it.second) }
            return@withUserLock FarmOperationResult(false, "卖出失败，果实已退回仓库")
        }
        FarmOperationResult(true, "卖出果实${sold}个，获得${totalMoney}金币")
    }

    fun upgradeFarm(user: User): FarmOperationResult = withUserLock(user.id) {
        val state = getOrCreateFarm(user.id)
        val player = state.player
        if (player.level >= FarmConstants.MAX_LEVEL) {
            return@withUserLock FarmOperationResult(false, "农场已经满级")
        }
        val nextLevel = player.level + 1
        val crop = FarmCropManager.getCropByLevel(nextLevel)
            ?: return@withUserLock FarmOperationResult(false, "缺少${nextLevel}级农场升级配置")
        val cost = crop.upgradeCost.toDouble()
        if (EconomyUtil.getMoneyByUser(user) < cost) {
            return@withUserLock FarmOperationResult(false, "金币不足，升级需要${cost.toLong()}金币")
        }
        if (!EconomyUtil.minusMoneyToUser(user, cost)) {
            return@withUserLock FarmOperationResult(false, "扣款失败")
        }
        player.level = nextLevel
        FarmRepository.savePlayer(player)
        ensurePlots(player)
        FarmOperationResult(true, "农场升级成功：${nextLevel - 1} -> $nextLevel，花费${cost.toLong()}金币")
    }

    fun activateShield(qq: Long): FarmOperationResult = withUserLock(qq) {
        val player = getOrCreateFarm(qq).player
        if (player.level < 17) return@withUserLock FarmOperationResult(false, "17级开放激活守护")
        player.shieldUntil = System.currentTimeMillis() + 12 * 60 * MINUTE
        FarmRepository.savePlayer(player)
        FarmOperationResult(true, "守护已激活，持续12小时")
    }

    fun water(waterer: UserInfoDto, targetQq: Long): FarmOperationResult = withTwoUserLocks(waterer.qq, targetQq) {
        if (waterer.qq == targetQq) return@withTwoUserLocks FarmOperationResult(false, "不能给自己浇水")
        val watererPlayer = getOrCreateFarm(waterer.qq).player
        if (watererPlayer.level < 13) return@withTwoUserLocks FarmOperationResult(false, "13级开放帮浇水")
        refreshWaterCounter(watererPlayer)
        val maxWater = if (watererPlayer.level >= 18) 10 else 5
        if (watererPlayer.todayWaterCount >= maxWater) {
            return@withTwoUserLocks FarmOperationResult(false, "今日浇水次数已用完")
        }

        val targetState = getOrCreateFarm(targetQq)
        val now = System.currentTimeMillis()
        val plot = targetState.plots
            .filter { it.status == FarmConstants.PLOT_PLANTED && it.nextMatureAt > now }
            .maxByOrNull { it.nextMatureAt - now }
            ?: return@withTwoUserLocks FarmOperationResult(false, "对方没有需要浇水的作物")
        val crop = FarmCropManager.getCrop(plot.cropCode)
            ?: return@withTwoUserLocks FarmOperationResult(false, "目标作物数据异常")

        val remaining = plot.nextMatureAt - now
        val reduceMillis = if (crop.level < 10) {
            RandomUtil.randomInt(5, 16) * MINUTE
        } else {
            val fixed = RandomUtil.randomInt(20, 41) * MINUTE
            val percent = RandomUtil.randomInt(1, 6)
            fixed + (remaining * percent / 100.0).roundToLong()
        }.coerceAtMost(remaining)
        plot.nextMatureAt = (plot.nextMatureAt - reduceMillis).coerceAtLeast(now)
        FarmRepository.savePlot(plot)

        watererPlayer.todayWaterCount += 1
        FarmRepository.savePlayer(watererPlayer)

        val dropMessage = dropWaterReward(waterer, crop)
        val reduceMinutes = (reduceMillis / MINUTE).coerceAtLeast(1)
        FarmOperationResult(true, "浇水成功，${crop.name} 缩短${reduceMinutes}分钟成熟时间$dropMessage")
    }

    private fun ensurePlots(player: FarmPlayer): List<FarmPlot> {
        val existing = FarmRepository.listPlots(player.qq).associateBy { it.plotNo }
        val unlocked = unlockedPlotCount(player.level)
        val plots = (1..FarmConstants.MAX_PLOTS).map { plotNo ->
            val plot = existing[plotNo] ?: FarmPlot().apply {
                qq = player.qq
                this.plotNo = plotNo
            }
            if (plot.status == FarmConstants.PLOT_LOCKED && plotNo <= unlocked) {
                plot.status = FarmConstants.PLOT_EMPTY
            }
            if (plot.id == 0L) {
                plot.status = if (plotNo <= unlocked) FarmConstants.PLOT_EMPTY else FarmConstants.PLOT_LOCKED
            }
            FarmRepository.savePlot(plot)
        }
        return plots.sortedBy { it.plotNo }
    }

    private fun clearPlot(plot: FarmPlot) {
        plot.status = FarmConstants.PLOT_EMPTY
        plot.cropCode = ""
        plot.plantedAt = 0
        plot.currentSeason = 0
        plot.nextMatureAt = 0
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

        BackpackManager.addStackablePropToBackpack(waterer, reward.first, PropsKind.functionProp, reward.second)
        val name = if (reward.first == FunctionProps.FARM_RAFFLE_BASIC) "初级农场抽奖券" else "高级农场抽奖券"
        return "，获得${name} x${reward.second}"
    }
}
