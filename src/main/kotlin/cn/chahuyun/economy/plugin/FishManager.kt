package cn.chahuyun.economy.plugin

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.fish.FishDto
import cn.chahuyun.economy.repository.FishRepository
import cn.hutool.poi.excel.ExcelReader
import cn.hutool.poi.excel.ExcelUtil
import java.io.InputStream

/**
 * 楸肩鐞? */
object FishManager {
    /**
     * 褰掔被鏁寸悊鐨勯奔
     */
    private val fishMap: MutableMap<Int, MutableList<FishDto>> = HashMap()

    @JvmStatic
    fun init() {
        val fishList = fishProxy.findAll()
        if (fishList.isEmpty()) {
            reloadFish()
            return
        }
        readFish(fishList)

        FishRepository.resetAllFishingStatus()
    }

    /**
     * 鑾峰彇瀵瑰簲鐨勯奔绛夌骇
     */
    @JvmStatic
    fun getLevelFishList(fishLevel: Int): List<FishDto> {
        return fishMap[fishLevel] ?: emptyList()
    }

    /**
     * 浠?resources 閲岄潰璇诲彇楸兼暟鎹?     */
    private fun reloadFish() {
        val instance = HuYanEconomy
        val resourceAsStream: InputStream = instance.getResourceAsStream("fish.xls") ?: return
        val map = hashMapOf(
            "绛夌骇" to "level",
            "鍚嶇О" to "name",
            "鎻忚堪" to "description",
            "鍗曚环" to "price",
            "最小尺寸" to "dimensionsMin",
            "最大尺寸" to "dimensionsMax",
            "尺寸1阈值" to "dimensions1",
            "尺寸2阈值" to "dimensions2",
            "尺寸3阈值" to "dimensions3",
            "尺寸4阈值" to "dimensions4",
            "闅惧害" to "difficulty",
            "鐗规畩鏍囪" to "special"
        )
        val reader: ExcelReader = ExcelUtil.getReader(resourceAsStream)
        val fishList: List<FishDto> = reader.setHeaderAlias(map).readAll(FishDto::class.java)
        fishProxy.saveAll(fishList)
        readFish(fishList)
    }

    private fun readFish(list: List<FishDto>) {
        fishMap.clear()
        for (fish in list) {
            val level = fish.level
            fishMap.getOrPut(level) { mutableListOf() }.add(fish)
        }
    }

    private val fishProxy
        get() = EntityProxyRegistry.get<FishDto>("fish") ?: error("鱼种代理器未初始化")
}
