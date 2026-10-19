package cn.chahuyun.economy.game.fishing

import cn.chahuyun.economy.actionchain.annotation.Action
import cn.chahuyun.economy.actionchain.annotation.Before
import cn.chahuyun.economy.game.fishing.FishingChain.fish
import cn.chahuyun.economy.utils.Log
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 钓鱼规则链入口（阶段四试点，全新包）。
 *
 * 钓鱼玩法后续会按新实体与规则链重建，因此这里另起 `game.fishing` 包，不复用
 * [cn.chahuyun.economy.usecase.FishingUsecase] 的旧入口。当前只打印日志，用于验证：
 * - economy-game 的 KSP 代码生成（facade + `EconomyGameActionModule`）
 * - economy-main 的模块聚合与 `ActionRuntime` 安装
 * - `@Before` / `@Action` 在真实模块中的执行顺序
 *
 * 重建钓鱼时把真正的玩法逻辑搬进 [fish]，再移除指令入口里的旧 usecase 调用。
 */
object FishingChain {

    /** 试点前置规则：仅打印日志，用于确认 before 阶段按 order 执行。 */
    @Before(actionId = "game.fish", order = 10)
    suspend fun beforeFish() {
        Log.info("[actionchain] game.fish before 规则触发")
    }

    /**
     * 钓鱼动作本体（占位）。
     *
     * 当前只打印一行日志，不改变任何业务状态。
     */
    @Action("game.fish")
    suspend fun fish(event: GroupMessageEvent) {
        Log.info(
            "[actionchain] game.fish 执行：群 ${event.group.id} 用户 ${event.sender.id}" +
                "（钓鱼玩法待重建，当前仅打印日志）"
        )
    }
}
