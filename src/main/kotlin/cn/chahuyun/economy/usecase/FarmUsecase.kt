package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.config.EconomyConfig
import cn.chahuyun.economy.constant.FarmConstants
import cn.chahuyun.economy.entity.farm.FarmCrop
import cn.chahuyun.economy.entity.farm.FarmPlot
import cn.chahuyun.economy.manager.FarmManager
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.plugin.FarmCropManager
import cn.chahuyun.economy.repository.FarmRepository
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import java.util.concurrent.TimeUnit

object FarmUsecase {

    suspend fun viewFarm(event: GroupMessageEvent) {
        val state = FarmManager.getOrCreateFarm(event.sender.id)
        event.subject.sendMessage(
            MessageUtil.formatMessageChain(
                event.message,
                buildString {
                    append("农场等级:${state.player.level}\n")
                    val shield = state.player.shieldUntil - System.currentTimeMillis()
                    if (shield > 0) append("守护剩余:${formatDuration(shield)}\n")
                    append("土地:\n")
                    append(renderPlots(state.plots))
                }
            )
        )
    }

    suspend fun viewShop(event: GroupMessageEvent) {
        val player = FarmManager.getOrCreateFarm(event.sender.id).player
        val crops = FarmCropManager.listCropsForLevel(player.level)
        val text = if (crops.isEmpty()) {
            "农场商店暂无可购买种子"
        } else {
            crops.joinToString("\n", "农场商店(${player.level}级可购买):\n") {
                "${it.emoji} ${it.name} 种子:${it.seedPrice}金币 成熟:${it.firstMatureMinutes}分 售价:${it.fruitPrice}"
            }
        }
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, text))
    }

    suspend fun viewWarehouse(event: GroupMessageEvent) {
        FarmManager.getOrCreateFarm(event.sender.id)
        val items = FarmRepository.listInventory(event.sender.id)
        val text = if (items.isEmpty()) {
            "农场仓库是空的"
        } else {
            items.groupBy { it.itemType }.entries.joinToString("\n") { (type, list) ->
                val title = if (type == FarmConstants.ITEM_SEED) "种子" else "果实"
                list.joinToString("\n", "$title:\n") { inventory ->
                    val crop = FarmCropManager.getCrop(inventory.itemCode)
                    "${crop?.emoji ?: ""} ${crop?.name ?: inventory.itemCode} x${inventory.amount}"
                }
            }
        }
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, text))
    }

    suspend fun buySeed(event: GroupMessageEvent) {
        val raw = commandText(event).removePrefix("购买种子").trim()
        val (crop, amount, cropName) = parseCropAndAmount(raw)
        if (crop == null) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "作物不存在: $cropName"))
            return
        }
        val result = FarmManager.buySeed(event.sender, crop, amount)
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, result.message))
    }

    suspend fun plant(event: GroupMessageEvent) {
        val raw = commandText(event).removePrefix("播种").trim()
        val tokens = raw.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.size < 2) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "格式: 播种 <土地编号...> <作物名>"))
            return
        }
        val cropName = tokens.last()
        val crop = FarmCropManager.getCrop(cropName)
        if (crop == null) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "作物不存在: $cropName"))
            return
        }
        val plotNumbers = FarmManager.parsePlotNumbers(tokens.dropLast(1).joinToString(" "))
        if (plotNumbers.isEmpty()) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "没有有效土地编号"))
            return
        }
        val result = FarmManager.plant(event.sender, plotNumbers, crop)
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, result.message))
    }

    suspend fun harvest(event: GroupMessageEvent) {
        val raw = commandText(event).removePrefix("收获").trim()
        val plotNumbers = FarmManager.parsePlotNumbers(raw)
        if (plotNumbers.isEmpty()) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "没有有效土地编号"))
            return
        }
        val result = FarmManager.harvest(event.sender.id, plotNumbers)
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, result.message))
    }

    suspend fun sellFruits(event: GroupMessageEvent) {
        val raw = commandText(event).removePrefix("卖出果实").trim()
        val tokens = raw.split(Regex("\\s+")).filter { it.isNotBlank() }
        val target = tokens.getOrNull(0).orEmpty()
        if (target.isBlank()) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "格式: 卖出果实 <作物名|全部> [数量]"))
            return
        }
        val amount = tokens.lastOrNull()?.toIntOrNull()
        val crop = if (target == "全部") null else parseCropAndAmount(raw).crop
        if (target != "全部" && crop == null) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "作物不存在: ${raw.removeSuffix(amount?.toString().orEmpty()).trim()}"))
            return
        }
        val result = FarmManager.sellFruits(event.sender, crop, amount)
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, result.message))
    }

    suspend fun upgradeFarm(event: GroupMessageEvent) {
        val result = FarmManager.upgradeFarm(event.sender)
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, result.message))
    }

    suspend fun water(event: GroupMessageEvent) {
        val at = event.message.filterIsInstance<At>().firstOrNull()
        if (at == null) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "格式: 帮浇水 @用户"))
            return
        }
        val userInfo = UserCoreManager.getUserInfo(event.sender)
        val result = FarmManager.water(userInfo, at.target)
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, result.message))
    }

    suspend fun sellAll(event: GroupMessageEvent) {
        val player = FarmManager.getOrCreateFarm(event.sender.id).player
        if (player.level < 14) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "14级开放一键卖出"))
            return
        }
        val result = FarmManager.sellFruits(event.sender, null, null)
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, result.message))
    }

    suspend fun harvestAll(event: GroupMessageEvent) {
        val state = FarmManager.getOrCreateFarm(event.sender.id)
        if (state.player.level < 15) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "15级开放一键收获"))
            return
        }
        val now = System.currentTimeMillis()
        val plotNumbers = state.plots
            .filter { it.status == FarmConstants.PLOT_PLANTED && it.nextMatureAt <= now }
            .map { it.plotNo }
        if (plotNumbers.isEmpty()) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "没有成熟作物可收获"))
            return
        }
        val result = FarmManager.harvest(event.sender.id, plotNumbers)
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, result.message))
    }

    suspend fun plantAll(event: GroupMessageEvent) {
        val cropName = commandText(event).removePrefix("一键播种").trim()
        val crop = findCrop(cropName)
        if (crop == null) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "作物不存在: $cropName"))
            return
        }
        val state = FarmManager.getOrCreateFarm(event.sender.id)
        if (state.player.level < 16) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "16级开放一键播种"))
            return
        }
        val plotNumbers = state.plots.filter { it.status == FarmConstants.PLOT_EMPTY }.map { it.plotNo }
        if (plotNumbers.isEmpty()) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "没有空闲土地可播种"))
            return
        }
        val result = FarmManager.plant(event.sender, plotNumbers, crop)
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, result.message))
    }

    suspend fun activateShield(event: GroupMessageEvent) {
        val result = FarmManager.activateShield(event.sender.id)
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, result.message))
    }

    suspend fun blackMarket(event: GroupMessageEvent) {
        val player = FarmManager.getOrCreateFarm(event.sender.id).player
        val message = if (player.level < 18) "18级开放黑市" else "黑市尚未开放"
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, message))
    }

    private fun renderPlots(plots: List<FarmPlot>): String {
        val now = System.currentTimeMillis()
        return plots.chunked(6).joinToString("\n") { row ->
            row.joinToString(" ") { plot ->
                val status = when (plot.status) {
                    FarmConstants.PLOT_LOCKED -> "锁"
                    FarmConstants.PLOT_EMPTY -> "空"
                    else -> {
                        if (plot.nextMatureAt <= now) "熟" else "苗"
                    }
                }
                "%02d:%s".format(plot.plotNo, status)
            }
        }
    }

    private fun commandText(event: GroupMessageEvent): String {
        val text = event.message.contentToString().trim()
        val prefixes = listOf(EconomyConfig.prefix, "#").filter { it.isNotBlank() }.distinct()
        prefixes.firstOrNull { text.startsWith(it) }?.let {
            return text.removePrefix(it).trimStart()
        }
        return text
    }

    private data class CropAmount(
        val crop: FarmCrop?,
        val amount: Int,
        val rawCropName: String,
    )

    private fun parseCropAndAmount(raw: String): CropAmount {
        val tokens = raw.split(Regex("\\s+")).filter { it.isNotBlank() }
        val amount = tokens.lastOrNull()?.toIntOrNull() ?: 1
        val cropTokens = if (tokens.lastOrNull()?.toIntOrNull() != null) tokens.dropLast(1) else tokens
        val rawCropName = cropTokens.joinToString(" ")
        return CropAmount(findCrop(rawCropName), amount, rawCropName)
    }

    private fun findCrop(raw: String): FarmCrop? {
        val normalized = raw.trim()
        if (normalized.isBlank()) return null
        FarmCropManager.getCrop(normalized)?.let { return it }

        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        for (start in tokens.indices) {
            val candidate = tokens.drop(start).joinToString(" ")
            FarmCropManager.getCrop(candidate)?.let { return it }
        }
        return tokens.asReversed().firstNotNullOfOrNull { FarmCropManager.getCrop(it) }
    }

    private fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return "${hours}小时${minutes}分钟"
    }
}
