package cn.chahuyun.economy.plugin

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.entity.farm.FarmCrop
import cn.chahuyun.economy.repository.FarmRepository
import cn.chahuyun.economy.utils.Log
import cn.hutool.poi.excel.ExcelUtil

object FarmCropManager {
    private val cropsByCode = linkedMapOf<String, FarmCrop>()
    private val cropsByName = linkedMapOf<String, FarmCrop>()
    private val cropsByLevel = linkedMapOf<Int, FarmCrop>()

    @JvmStatic
    fun init() {
        val crops = FarmRepository.listCrops()
        if (crops.isEmpty()) {
            reloadCrops()
        } else {
            readCrops(crops)
        }
    }

    @JvmStatic
    fun getCrop(codeOrName: String): FarmCrop? = cropsByCode[codeOrName] ?: cropsByName[codeOrName]

    @JvmStatic
    fun getCropByLevel(level: Int): FarmCrop? = cropsByLevel[level]

    @JvmStatic
    fun listCrops(): List<FarmCrop> = cropsByLevel.values.toList()

    @JvmStatic
    fun listCropsForLevel(level: Int): List<FarmCrop> = cropsByLevel.values.filter { it.level <= level }

    private fun reloadCrops() {
        val input = HuYanEconomy.getResourceAsStream("种子.xlsx")
        if (input == null) {
            Log.warning("农场作物资源 种子.xlsx 不存在，农场商店将为空")
            return
        }

        val reader = ExcelUtil.getReader(input)
        val rows = reader.readAll()
        val crops = rows.mapNotNull { row ->
            val level = intValue(row["等级"]) ?: return@mapNotNull null
            val name = stringValue(row["作物名称"]).takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val matureText = stringValue(row["基础成熟时间"])
            FarmCrop().apply {
                this.code = "farm-crop-$level"
                this.level = level
                this.upgradeCost = longValue(row["升级消耗"]) ?: 0L
                this.name = name
                this.emoji = stringValue(row["Emoji"])
                this.seedPrice = intValue(row["种子买入价"]) ?: 0
                this.totalSeasons = firstNumber(stringValue(row["总产季"])).coerceAtLeast(1)
                this.yieldPerSeason = firstNumber(stringValue(row["每季数量"])).coerceAtLeast(1)
                this.fruitPrice = intValue(row["单个果实售价"]) ?: 0
                this.firstMatureMinutes = parseMinutes(matureText, "初熟").coerceAtLeast(1)
                this.nextMatureMinutes = parseMinutes(matureText, "再熟")
                this.totalRevenue = intValue(row["播种总收益"]) ?: 0
                this.pureProfit = intValue(row["最终纯利润"]) ?: 0
                this.roi = doubleValue(row["综合 ROI"]) ?: 0.0
            }
        }
        FarmRepository.saveCrops(crops)
        readCrops(crops)
        Log.info("农场作物资源导入完成: ${crops.size} 条")
    }

    private fun readCrops(crops: List<FarmCrop>) {
        cropsByCode.clear()
        cropsByName.clear()
        cropsByLevel.clear()
        crops.sortedBy { it.level }.forEach {
            cropsByCode[it.code] = it
            cropsByName[it.name] = it
            cropsByLevel[it.level] = it
        }
    }

    private fun stringValue(value: Any?): String = value?.toString()?.trim() ?: ""

    private fun intValue(value: Any?): Int? = doubleValue(value)?.toInt()

    private fun longValue(value: Any?): Long? = doubleValue(value)?.toLong()

    private fun doubleValue(value: Any?): Double? {
        val text = stringValue(value).replace(",", "")
        if (text.isBlank() || text == "—" || text == "-") return null
        return text.toDoubleOrNull()
    }

    private fun firstNumber(text: String): Int =
        Regex("\\d+").find(text)?.value?.toIntOrNull() ?: 0

    private fun parseMinutes(text: String, label: String): Int {
        return Regex("$label:\\s*(\\d+)分").find(text)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }
}
