package cn.chahuyun.economy.game

/**
 * 游戏概览桥接。
 *
 * core 模块通过该接口获取 game 模块提供的概览文本，避免 core 反向依赖 game。
 * 当前仅保留农场状态；钓鱼已下线，待第三代钓鱼重写后再补充对应能力。
 */
interface GameOverviewProvider {
    fun farmStatusText(qq: Long): String
}

object GameOverviewBridge {
    @Volatile
    private var provider: GameOverviewProvider? = null

    fun register(provider: GameOverviewProvider) {
        this.provider = provider
    }

    fun farmStatusText(qq: Long): String {
        return provider?.farmStatusText(qq) ?: "未种植"
    }
}
