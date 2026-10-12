package cn.chahuyun.economy.game

import cn.chahuyun.economy.service.GameOverviewService

object DefaultGameOverviewProvider : GameOverviewProvider {
    override fun farmStatusText(qq: Long): String {
        return GameOverviewService.farmStatusText(qq)
    }
}
