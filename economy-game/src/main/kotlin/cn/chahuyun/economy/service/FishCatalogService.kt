package cn.chahuyun.economy.service

import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.data.repository.FishRepository
import cn.chahuyun.economy.model.fish.FishDto
import cn.chahuyun.economy.runtime.EconomyRuntime
import cn.hutool.poi.excel.ExcelReader
import cn.hutool.poi.excel.ExcelUtil
import java.io.InputStream

object FishCatalogService {
    private val fishMap: MutableMap<Int, MutableList<FishDto>> = HashMap()

    fun initCatalog() {
        val fishList = fishProxy.findAll()
        if (fishList.isEmpty()) {
            reloadFish()
            return
        }
        readFish(fishList)

        FishRepository.resetAllFishingStatus()
    }

    fun levelFishList(fishLevel: Int): List<FishDto> =
        fishMap[fishLevel] ?: emptyList()

    private fun reloadFish() {
        val resourceAsStream: InputStream = EconomyRuntime.plugin.getResourceAsStream("fish.xls") ?: return
        val map = hashMapOf(
            "等级" to "level",
            "名称" to "name",
            "描述" to "description",
            "单价" to "price",
            "最小尺寸" to "dimensionsMin",
            "最大尺寸" to "dimensionsMax",
            "尺寸1阶" to "dimensions1",
            "尺寸2阶" to "dimensions2",
            "尺寸3阶" to "dimensions3",
            "尺寸4阶" to "dimensions4",
            "难度" to "difficulty",
            "特殊标记" to "special"
        )
        val reader: ExcelReader = ExcelUtil.getReader(resourceAsStream)
        val fishList: List<FishDto> = reader.setHeaderAlias(map).readAll(FishDto::class.java)
        fishProxy.saveAll(fishList)
        readFish(fishList)
    }

    private fun readFish(list: List<FishDto>) {
        fishMap.clear()
        for (fish in list) {
            fishMap.getOrPut(fish.level) { mutableListOf() }.add(fish)
        }
    }

    private val fishProxy
        get() = EntityProxyRegistry.get<FishDto>("fish") ?: error("鱼种代理器未初始化")
}
