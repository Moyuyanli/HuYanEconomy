package cn.chahuyun.economy.games.farm

import cn.chahuyun.economy.actionchain.annotation.Before
import cn.chahuyun.economy.actionchain.annotation.FromProvider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.actionchain.runtime.ActionResult
import net.mamoe.mirai.message.data.At

/**
 * 农场功能的规则链：`@Before` 前置规则集合。
 *
 * 农场十八个动作里，只有三个真正需要「命令层解析」：
 * ```
 * game.farm.water    @Before(10) resolveWater   解析 @目标 与次数（`帮浇水 @张三 *3`）
 * game.farm.steal    @Before(10) resolveSteal   解析 @目标
 * game.farm.upgrade  @Before(10) resolveUpgrade 解析「逐级升级」标记（指令末尾的 `*`）
 * ```
 *
 * 其余动作（买种子/播种/收获/卖出/一键系列）的参数解析与等级门槛已经在
 * [FarmCommandService] 里，返回 `FarmOperationResult` 自带失败文案；
 * 把同一套解析再写进规则会造成两处口径，因此这里**不重复搬**。
 *
 * 农场的命令解析、等级门槛与并发锁全部在 [FarmCommandService] 内，
 * 规则不依赖任何 Provider 之外的额外数据源。
 */
object FarmRules {

    /**
     * 前置规则（order=10）：解析「帮浇水」的目标与次数。
     *
     * 次数写法是指令末尾的 `*N`（例如 `帮浇水 @张三 *3`），缺省为 1 次。
     *
     * @param context 本次调用上下文，用于放入解析结果。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "game.farm.water", order = 10)
    suspend fun resolveWater(
        context: ActionContext,
        @FromProvider(FarmProvider.ACTOR) actor: FarmActor,
    ): ActionResult {
        val targetQq = actor.atTarget()
            ?: return ActionResult.EndWithMessage("格式: 帮浇水 @用户")
        val times = waterTimes(actor.commandText())
        context.put(FarmWaterIntent::class, FarmWaterIntent(targetQq = targetQq, times = times))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：解析「偷菜」的目标。
     *
     * @param context 本次调用上下文，用于放入解析结果。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "game.farm.steal", order = 10)
    suspend fun resolveSteal(
        context: ActionContext,
        @FromProvider(FarmProvider.ACTOR) actor: FarmActor,
    ): ActionResult {
        val targetQq = actor.atTarget()
            ?: return ActionResult.EndWithMessage("格式: 偷菜 @用户")
        context.put(FarmTargetIntent::class, FarmTargetIntent(targetQq = targetQq))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：识别「升级农场*」的逐级升级写法。
     *
     * 指令末尾带 `*` 表示一直升到失败为止，否则只升一级。此项解析不会失败。
     *
     * @param context 本次调用上下文，用于放入解析结果。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "game.farm.upgrade", order = 10)
    suspend fun resolveUpgrade(
        context: ActionContext,
        @FromProvider(FarmProvider.ACTOR) actor: FarmActor,
    ): ActionResult {
        val auto = actor.message.contentToString().trim().endsWith("*")
        context.put(FarmUpgradeIntent::class, FarmUpgradeIntent(auto = auto))
        return ActionResult.Continue
    }

    /** 取消息里第一个 @ 的目标 QQ；没有 @ 返回 null。 */
    private fun FarmActor.atTarget(): Long? =
        message.filterIsInstance<At>().firstOrNull()?.target

    /** 去掉指令前缀后的正文，`帮浇水 @张三 *3` → `帮浇水 @张三 *3`。 */
    private fun FarmActor.commandText(): String =
        FarmCommandTextParser.commandText(message.contentToString())

    /**
     * 从正文末尾解析浇水次数：`*3` → 3，无法解析或小于 1 时按 1 次处理。
     */
    private fun waterTimes(text: String): Int =
        Regex("\\*(\\d+)\\s*$")
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.coerceAtLeast(1)
            ?: 1
}

/**
 * 一次「帮浇水」的目标与次数。
 *
 * @property targetQq 被浇水的用户 QQ。
 * @property times 浇水次数（≥1）。
 */
data class FarmWaterIntent(
    val targetQq: Long,
    val times: Int,
)

/**
 * 一次「偷菜」的目标。
 *
 * @property targetQq 被偷的用户 QQ。
 */
data class FarmTargetIntent(
    val targetQq: Long,
)

/**
 * 一次「升级农场」的升级方式。
 *
 * @property auto true 表示逐级升到失败为止，false 表示只升一级。
 */
data class FarmUpgradeIntent(
    val auto: Boolean,
)
