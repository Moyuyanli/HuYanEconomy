package cn.chahuyun.economy.service

import cn.chahuyun.economy.model.farm.FarmViewState

object FarmViewService {

    fun getOrCreateViewState(qq: Long): FarmViewState =
        FarmViewStateService.getOrCreateViewState(qq)

    fun renderFarm(state: FarmViewState, now: Long = System.currentTimeMillis()): String =
        FarmTextRenderService.renderFarm(state, now)

    fun renderShop(state: FarmViewState): String =
        FarmTextRenderService.renderShop(state)

    fun renderWarehouse(state: FarmViewState): String =
        FarmTextRenderService.renderWarehouse(state)

    fun blackMarketText(state: FarmViewState): String =
        FarmTextRenderService.blackMarketText(state)

    fun farmStatusText(state: FarmViewState, now: Long = System.currentTimeMillis()): String =
        FarmTextRenderService.farmStatusText(state, now)
}
