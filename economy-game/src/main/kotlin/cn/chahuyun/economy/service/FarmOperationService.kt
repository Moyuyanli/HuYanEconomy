package cn.chahuyun.economy.service

import cn.chahuyun.economy.constant.FarmConstants
import cn.chahuyun.economy.entity.farm.FarmCrop
import cn.chahuyun.economy.entity.farm.FarmPlot
import cn.chahuyun.economy.model.farm.FarmOperationResult
import cn.chahuyun.economy.model.farm.FarmState
import net.mamoe.mirai.contact.User

object FarmOperationService {
    private const val MINUTE = 60_000L
    private const val SHIELD_REQUIRED_LEVEL = 17
    private const val SHIELD_DURATION_MILLIS = 12 * 60 * MINUTE

    fun upgradeFarm(user: User, state: FarmState): FarmOperationResult {
        val player = state.player
        if (player.level >= FarmConstants.MAX_LEVEL) {
            return FarmOperationResult(false, "农场已经满级")
        }
        val nextLevel = player.level + 1
        val crop = FarmCropService.getCropByLevel(nextLevel)
            ?: return FarmOperationResult(false, "缺少${nextLevel}级农场升级配置")
        val cost = crop.upgradeCost.toDouble()
        if (EconomyAccountService.walletBalance(user) < cost) {
            return FarmOperationResult(false, "金币不足，升级需要${cost.toLong()}金币")
        }
        if (!EconomyAccountService.subtractWallet(user, cost)) {
            return FarmOperationResult(false, "扣款失败")
        }
        player.level = nextLevel
        FarmPlayerService.savePlayer(player)
        FarmPlotService.ensurePlots(player)
        return FarmOperationResult(true, "农场升级成功，${nextLevel - 1} -> $nextLevel，花费${cost.toLong()}金币")
    }

    fun activateShield(state: FarmState): FarmOperationResult {
        val player = state.player
        if (player.level < SHIELD_REQUIRED_LEVEL) {
            return FarmOperationResult(false, "17级开放激活守护")
        }
        player.shieldUntil = System.currentTimeMillis() + SHIELD_DURATION_MILLIS
        FarmPlayerService.savePlayer(player)
        return FarmOperationResult(true, "守护已激活，持续12小时")
    }

    fun buySeed(user: User, cropCode: String, amount: Int, state: FarmState): FarmOperationResult {
        val crop = FarmCropService.getCrop(cropCode)
            ?: return FarmOperationResult(false, "作物不存在: $cropCode")
        if (amount <= 0) return FarmOperationResult(false, "购买数量必须大于0")
        if (state.player.level < crop.level) {
            return FarmOperationResult(false, "${crop.name} 需要农场${crop.level}级才能购买")
        }
        val cost = crop.seedPrice.toDouble() * amount
        if (EconomyAccountService.walletBalance(user) < cost) {
            return FarmOperationResult(false, "金币不足，需要${cost.toLong()}金币")
        }
        if (!EconomyAccountService.subtractWallet(user, cost)) {
            return FarmOperationResult(false, "扣款失败")
        }
        runCatching {
            FarmInventoryStorageService.addInventory(user.id, FarmConstants.ITEM_SEED, crop.code, amount)
        }.onFailure {
            EconomyAccountService.addWallet(user, cost)
            return FarmOperationResult(false, "种子入库失败，已退款")
        }
        return FarmOperationResult(true, "购买 ${crop.name} 种子 x$amount 成功，花费${cost.toLong()}金币")
    }

    fun plant(user: User, plotNumbers: List<Int>, cropCode: String, state: FarmState): FarmOperationResult {
        val crop = FarmCropService.getCrop(cropCode)
            ?: return FarmOperationResult(false, "作物不存在: $cropCode")
        if (state.player.level < crop.level) {
            return FarmOperationResult(false, "${crop.name} 需要农场${crop.level}级才能播种")
        }
        val plotsByNo = state.plots.associateBy { it.plotNo }
        val plantable = plotNumbers.mapNotNull { plotsByNo[it] }
            .filter { it.status == FarmConstants.PLOT_EMPTY }
        if (plantable.isEmpty()) {
            return FarmOperationResult(false, "没有可播种的空闲土地")
        }
        val seedAmount = FarmInventoryStorageService.inventoryAmount(user.id, FarmConstants.ITEM_SEED, crop.code)
        val selected = plantable.take(seedAmount)
        if (selected.isEmpty()) {
            return FarmOperationResult(false, "${crop.name} 种子不足")
        }
        FarmInventoryStorageService.removeInventory(user.id, FarmConstants.ITEM_SEED, crop.code, selected.size)
        val now = System.currentTimeMillis()
        selected.forEach { plot ->
            plot.status = FarmConstants.PLOT_PLANTED
            plot.cropCode = crop.code
            plot.plantedAt = now
            plot.currentSeason = 1
            plot.nextMatureAt = now + crop.firstMatureMinutes * MINUTE
            FarmPlotService.savePlot(plot)
        }
        val skipped = plantable.size - selected.size
        val suffix = if (skipped > 0) "，种子不足跳过${skipped}块" else ""
        return FarmOperationResult(true, "播种 ${crop.name} x${selected.size} 成功$suffix")
    }

    fun harvest(qq: Long, plotNumbers: List<Int>, state: FarmState): FarmOperationResult {
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
            val crop = FarmCropService.getCrop(plot.cropCode)
            if (crop == null) {
                messages += "${plotNo}号地作物数据异常"
                return@forEach
            }
            FarmInventoryStorageService.addInventory(qq, FarmConstants.ITEM_FRUIT, crop.code, crop.yieldPerSeason)
            harvested += 1
            fruits += crop.yieldPerSeason
            if (plot.currentSeason < crop.totalSeasons && crop.nextMatureMinutes > 0) {
                plot.currentSeason += 1
                plot.nextMatureAt = now + crop.nextMatureMinutes * MINUTE
            } else {
                clearPlot(plot)
            }
            FarmPlotService.savePlot(plot)
        }

        return if (harvested == 0) {
            FarmOperationResult(false, messages.joinToString("\n").ifBlank { "没有收获任何作物" })
        } else {
            val detail = if (messages.isEmpty()) "" else "\n" + messages.joinToString("\n")
            FarmOperationResult(true, "收获${harvested}块土地，获得果实${fruits}个$detail")
        }
    }

    fun sellFruits(user: User, cropCode: String?, amount: Int?): FarmOperationResult {
        val crop = cropCode?.let {
            FarmCropService.getCrop(it) ?: return FarmOperationResult(false, "作物不存在: $it")
        }
        val inventories = FarmInventoryStorageService.listFruits(user.id)
        val targets = if (crop == null) inventories else inventories.filter { it.itemCode == crop.code }
        if (targets.isEmpty()) return FarmOperationResult(false, "没有可卖出的果实")

        var totalMoney = 0L
        var sold = 0
        val deductions = mutableListOf<Triple<String, Int, FarmCrop>>()
        for (inventory in targets) {
            val targetCrop = FarmCropService.getCrop(inventory.itemCode) ?: continue
            val sellAmount = if (crop == null) inventory.amount else (amount ?: inventory.amount).coerceAtMost(inventory.amount)
            if (sellAmount <= 0) continue
            totalMoney += sellAmount.toLong() * targetCrop.fruitPrice
            sold += sellAmount
            deductions += Triple(inventory.itemCode, sellAmount, targetCrop)
            if (crop != null) break
        }
        if (sold == 0 || totalMoney <= 0) return FarmOperationResult(false, "没有可卖出的果实")
        deductions.forEach {
            FarmInventoryStorageService.removeInventory(user.id, FarmConstants.ITEM_FRUIT, it.first, it.second)
        }
        if (!EconomyAccountService.addWallet(user, totalMoney.toDouble())) {
            deductions.forEach {
                FarmInventoryStorageService.addInventory(user.id, FarmConstants.ITEM_FRUIT, it.first, it.second)
            }
            return FarmOperationResult(false, "卖出失败，果实已退回仓库")
        }
        return FarmOperationResult(true, "卖出果实${sold}个，获得${totalMoney}金币")
    }

    fun clearPlot(plot: FarmPlot) {
        plot.status = FarmConstants.PLOT_EMPTY
        plot.cropCode = ""
        plot.plantedAt = 0
        plot.currentSeason = 0
        plot.nextMatureAt = 0
    }
}
