package cn.chahuyun.economy.games.farm

import cn.chahuyun.economy.game.GameOverviewProvider

object FarmGameOverviewProvider : GameOverviewProvider {
    override fun farmStatusText(qq: Long): String {
        return FarmOverviewService.farmStatusText(qq)
    }
}
