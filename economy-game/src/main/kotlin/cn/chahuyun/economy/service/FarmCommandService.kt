package cn.chahuyun.economy.service

import cn.chahuyun.economy.manager.FarmManager
import cn.chahuyun.economy.model.farm.FarmOperationResult
import net.mamoe.mirai.contact.User

object FarmCommandService {

    fun buySeed(user: User, raw: String): FarmOperationResult {
        val command = FarmCommandParser.parseCropAmount(raw)
        val cropView = command.cropView
            ?: return FarmOperationResult(false, "作物不存在: ${command.rawCropName}")
        return FarmManager.buySeed(user, cropView.code, command.amount)
    }

    fun plant(user: User, raw: String): FarmOperationResult {
        val command = FarmCommandParser.parsePlant(raw)
            ?: return FarmOperationResult(false, "格式: 播种 <土地编号...> <作物名>")
        val cropView = command.cropView
            ?: return FarmOperationResult(false, "作物不存在: ${command.rawCropName}")

        if (command.plotNumbers.isEmpty()) {
            return FarmOperationResult(false, "没有有效土地编号")
        }

        return FarmManager.plant(user, command.plotNumbers, cropView.code)
    }

    fun harvest(qq: Long, raw: String): FarmOperationResult {
        val plotNumbers = FarmCommandParser.parsePlotNumbers(raw)
        if (plotNumbers.isEmpty()) {
            return FarmOperationResult(false, "没有有效土地编号")
        }
        return FarmManager.harvest(qq, plotNumbers)
    }

    fun sellFruits(user: User, raw: String): FarmOperationResult {
        val command = FarmCommandParser.parseSell(raw)
            ?: return FarmOperationResult(false, "格式: 卖出果实 <作物名|全部> [数量]")
        val cropView = command.cropView
        if (!command.sellAll && cropView == null) {
            return FarmOperationResult(false, "作物不存在: ${command.rawCropName}")
        }

        return FarmManager.sellFruits(user, cropView?.code, command.amount)
    }

    fun sellAll(user: User): FarmOperationResult {
        val state = FarmViewService.getOrCreateViewState(user.id)
        if (!state.hasLevel(14)) {
            return FarmOperationResult(false, "14级开放一键卖出")
        }
        return FarmManager.sellFruits(user, null, null)
    }

    fun harvestAll(qq: Long, now: Long = System.currentTimeMillis()): FarmOperationResult {
        val state = FarmViewService.getOrCreateViewState(qq)
        if (!state.hasLevel(15)) {
            return FarmOperationResult(false, "15级开放一键收获")
        }

        val plotNumbers = state.readyPlotNumbers(now)
        if (plotNumbers.isEmpty()) {
            return FarmOperationResult(false, "没有成熟作物可收获")
        }

        return FarmManager.harvest(qq, plotNumbers)
    }

    fun plantAll(user: User, rawCropName: String): FarmOperationResult {
        val cropView = FarmCommandParser.parseCropName(rawCropName)
            ?: return FarmOperationResult(false, "作物不存在: $rawCropName")

        val state = FarmViewService.getOrCreateViewState(user.id)
        if (!state.hasLevel(16)) {
            return FarmOperationResult(false, "16级开放一键播种")
        }

        val plotNumbers = state.emptyPlotNumbers()
        if (plotNumbers.isEmpty()) {
            return FarmOperationResult(false, "没有空闲土地可播种")
        }

        return FarmManager.plant(user, plotNumbers, cropView.code)
    }
}
