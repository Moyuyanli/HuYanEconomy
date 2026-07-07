package cn.chahuyun.economy.service

import cn.chahuyun.economy.constant.FarmConstants
import cn.chahuyun.economy.model.farm.FarmCropView

data class FarmCropAmountCommand(
    val cropView: FarmCropView?,
    val amount: Int,
    val rawCropName: String,
)

data class FarmPlantCommand(
    val plotNumbers: List<Int>,
    val cropView: FarmCropView?,
    val rawCropName: String,
)

data class FarmSellCommand(
    val sellAll: Boolean,
    val cropView: FarmCropView?,
    val amount: Int?,
    val rawCropName: String,
)

object FarmCommandParser {

    fun parseCropAmount(raw: String): FarmCropAmountCommand {
        val parsed = FarmCropService.parseCropViewAndAmount(raw)
        return FarmCropAmountCommand(parsed.crop, parsed.amount, parsed.rawCropName)
    }

    fun parsePlant(raw: String): FarmPlantCommand? {
        val tokens = splitTokens(raw)
        if (tokens.size < 2) return null

        val rawCropName = tokens.last()
        val cropView = FarmCropService.getCropView(rawCropName)
        val plotNumbers = parsePlotNumbers(tokens.dropLast(1).joinToString(" "))
        return FarmPlantCommand(plotNumbers, cropView, rawCropName)
    }

    fun parseSell(raw: String): FarmSellCommand? {
        val tokens = splitTokens(raw)
        val target = tokens.getOrNull(0).orEmpty()
        if (target.isBlank()) return null

        val amount = tokens.lastOrNull()?.toIntOrNull()
        if (target == "全部") {
            return FarmSellCommand(sellAll = true, cropView = null, amount = amount, rawCropName = target)
        }

        val parsed = FarmCropService.parseCropViewAndAmount(raw)
        val rawCropName = if (amount == null) {
            raw.trim()
        } else {
            tokens.dropLast(1).joinToString(" ").ifBlank { raw.trim() }
        }
        return FarmSellCommand(sellAll = false, cropView = parsed.crop, amount = amount, rawCropName = rawCropName)
    }

    fun parseCropName(raw: String): FarmCropView? =
        FarmCropService.findCropView(raw)

    fun parsePlotNumbers(text: String): List<Int> {
        val normalized = text.replace('，', ',').replace(',', ' ')
        val numbers = linkedSetOf<Int>()
        splitTokens(normalized).forEach { token ->
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

    private fun splitTokens(raw: String): List<String> =
        raw.split(Regex("\\s+")).filter { it.isNotBlank() }
}
