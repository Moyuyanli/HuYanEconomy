package cn.chahuyun.economy.plugin

import cn.chahuyun.economy.model.fish.FishDto
import cn.chahuyun.economy.service.FishCatalogService

/**
 * 鱼种管理。
 */
object FishManager {
    @JvmStatic
    fun init() =
        FishCatalogService.initCatalog()

    /**
     * 获取对应等级的鱼种。
     */
    @JvmStatic
    fun getLevelFishList(fishLevel: Int): List<FishDto> =
        FishCatalogService.levelFishList(fishLevel)
}
