package cn.chahuyun.economy.service

import cn.chahuyun.economy.utils.Log

object GameOverviewService {

    fun farmStatusText(qq: Long): String {
        return try {
            FarmViewService.farmStatusText(FarmViewService.getOrCreateViewState(qq))
        } catch (e: Exception) {
            Log.error("获取农场状态失败", e)
            "未种植"
        }
    }
}
