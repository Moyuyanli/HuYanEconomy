package cn.chahuyun.economy.manager

import cn.chahuyun.economy.service.FishingCatalogService
import cn.chahuyun.economy.service.FishingRuntimeService
import cn.chahuyun.economy.service.GameCoroutineService
import cn.chahuyun.economy.utils.Log

/**
 * 钓鱼模块生命周期管理。
 */
object FishingManager {

    @JvmStatic
    fun init() {
        GameCoroutineService.init()
        FishingCatalogService.initCatalog()
        Log.info("钓鱼模块初始化完成")
    }

    @JvmStatic
    fun shutdown() {
        Log.info("钓鱼模块:正在关闭游戏线程...")
        FishingRuntimeService.resetAll()
        GameCoroutineService.shutdown()
    }
}
