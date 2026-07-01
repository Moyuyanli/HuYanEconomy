package cn.chahuyun.economy.plugin

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.model.fish.FishDto
import cn.chahuyun.economy.proxy.EntityProxyRegistry
import cn.chahuyun.economy.repository.FishRepository
import cn.hutool.poi.excel.ExcelReader
import cn.hutool.poi.excel.ExcelUtil
import java.io.InputStream

/**
 * 鱼管理
 */
object FishManager {
    /**
     * 归类整理的鱼
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
     * 获取对应的鱼等级
     */
    @JvmStatic
    fun getLevelFishList(fishLevel: Int): List<FishDto> {
        return fishMap[fishLevel] ?: emptyList()
    }

    /**
     * 从 resources 里面读取鱼数据
     */
    private fun reloadFish() {
        val instance = HuYanEconomy
        val resourceAsStream: InputStream = instance.getResourceAsStream("fish.xls") ?: return
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
            val level = fish.level
            fishMap.getOrPut(level) { mutableListOf() }.add(fish)
        }
    }

    private val fishProxy
        get() = EntityProxyRegistry.get<FishDto>("fish") ?: error("鱼种代理器未初始化")
}
