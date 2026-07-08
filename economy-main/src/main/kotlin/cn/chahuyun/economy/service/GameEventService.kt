package cn.chahuyun.economy.service

import cn.chahuyun.economy.fish.FishRollEvent
import cn.chahuyun.economy.fish.FishStartEvent
import cn.chahuyun.economy.usecase.GamesUsecase

/**
 * 游戏事件转发服务。
 *
 * main 模块负责订阅 mirai/自定义事件，game 模块负责玩法逻辑。
 * 这个服务把两者隔开，避免 action 或插件主类直接堆叠钓鱼流程细节。
 */
object GameEventService {

    /** 钓鱼开始事件：进入玩法 usecase，创建/刷新本轮钓鱼状态。 */
    suspend fun handleFishStart(event: FishStartEvent) {
        GamesUsecase.fishStart(event)
    }

    /** 钓鱼结算事件：进入玩法 usecase，完成随机鱼获与奖励结算。 */
    fun handleFishRoll(event: FishRollEvent) {
        GamesUsecase.fishRoll(event)
    }
}
