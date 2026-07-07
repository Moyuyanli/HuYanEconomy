package cn.chahuyun.economy.service

import cn.chahuyun.economy.entity.farm.FarmCrop
import cn.chahuyun.economy.model.farm.FarmCropView
import cn.chahuyun.economy.plugin.FarmCropManager

data class FarmCropViewAmount(
    val crop: FarmCropView?,
    val amount: Int,
    val rawCropName: String,
)

object FarmCropService {

    fun getCrop(codeOrName: String): FarmCrop? =
        FarmCropManager.getCrop(codeOrName)

    fun getCropByLevel(level: Int): FarmCrop? =
        FarmCropManager.getCropByLevel(level)

    fun getCropView(codeOrName: String): FarmCropView? =
        getCrop(codeOrName)?.toView()

    fun listCropViewsForLevel(level: Int): List<FarmCropView> =
        FarmCropManager.listCropsForLevel(level).map { it.toView() }

    fun parseCropViewAndAmount(raw: String): FarmCropViewAmount {
        val tokens = raw.split(Regex("\\s+")).filter { it.isNotBlank() }
        val amount = tokens.lastOrNull()?.toIntOrNull() ?: 1
        val cropTokens = if (tokens.lastOrNull()?.toIntOrNull() != null) tokens.dropLast(1) else tokens
        val rawCropName = cropTokens.joinToString(" ")
        return FarmCropViewAmount(findCropView(rawCropName), amount, rawCropName)
    }

    fun findCropView(raw: String): FarmCropView? {
        val normalized = raw.trim()
        if (normalized.isBlank()) return null
        getCropView(normalized)?.let { return it }

        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        for (start in tokens.indices) {
            val candidate = tokens.drop(start).joinToString(" ")
            getCropView(candidate)?.let { return it }
        }
        return tokens.asReversed().firstNotNullOfOrNull { getCropView(it) }
    }

    private fun FarmCrop.toView(): FarmCropView =
        FarmCropView(
            code = code,
            level = level,
            name = name,
            emoji = emoji,
            seedPrice = seedPrice,
            fruitPrice = fruitPrice,
            firstMatureMinutes = firstMatureMinutes,
        )
}
