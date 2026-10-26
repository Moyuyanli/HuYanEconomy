package cn.chahuyun.economy.feature.sign

import cn.chahuyun.economy.actionchain.annotation.After
import cn.chahuyun.economy.actionchain.annotation.Before
import cn.chahuyun.economy.actionchain.annotation.FromProvider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.actionchain.runtime.ActionResult
import cn.chahuyun.economy.feature.title.TitleManager
import cn.chahuyun.economy.runtime.EconomyRuntime
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 签到功能的规则链：`@Before` / `@After` 规则集合。
 *
 * **本文件取代了原来的 `SignEvent.kt`。** 签到不再广播 Mirai 自定义事件，需要挂在主流程前后的逻辑
 * 一律写在这里，由 KSP 编译期扫描并生成到 `EconomyCoreActionModule`，运行期不需要反射、
 * 也不需要 `economy-main` 手工订阅。
 *
 * ## 签到主链（`core.sign`）的执行顺序
 * ```
 * @Before(order=10) rejectIfNotSignable   资格校验：今天还没签到吗？
 * @Before(order=20) calculateReward       奖励计算：随机金币 + 称号/道具倍率
 * ------------------ SignUsecase.sign ------------------  推进状态、入账、扣道具、落库、发图
 * @After(order=10)  logCommitted          完成日志
 * @After(order=20)  grantSignTitles       称号检查（签到狂人 / 签到大王 / 首富）
 * ```
 *
 * 写法约定：
 * - 规则函数必须是 `suspend`，且只能返回 `Unit` 或 [ActionResult]；
 * - 参数只允许 `ActionContext` 或不可空的 Provider 类型（可按需加 `@FromProvider` 限定名）；
 * - `order` 越小越先执行，同 order 按 ruleId 字典序稳定排序；
 * - 前置拦截返回 [ActionResult.EndSilently] / [ActionResult.EndWithMessage]，主流程不会执行。
 */
object SignRules {

    // ==================================================================================
    // 一、core.sign 前置规则：能不能签、这次能给多少
    // ==================================================================================

    /**
     * 前置规则 1（order=10）：签到资格校验。
     *
     * 用 [SignService.canSign] 做**纯判断**——不改 `signTime` / `signNumber`，
     * 所以即使这里拦截，用户状态也保持原样，重复发指令不会把连签数搅乱。
     *
     * 放行返回 [ActionResult.Continue]；今天已经签过则结束本次调用并回一句提示，
     * 动作本体与后置规则都不会执行。
     *
     * 这是最典型的前置扩展点：将来要做「活动期间禁止签到」「黑名单静默拦截」，
     * 在本文件按 order 再插一条 `@Before` 即可，不用改 [SignUsecase]。
     *
     * @param actor 本次签到的参与者快照（由 [SignProvider.signer] 注入，同一次调用内只查一次库）。
     */
    @Before(actionId = "core.sign", order = 10)
    suspend fun rejectIfNotSignable(
        @FromProvider("signer") actor: SignActor,
    ): ActionResult {
        // 重置点的口径来自配置（默认 4 点），与 UserInfoDto.sign() 用的是同一个配置项。
        val signable = SignService.canSign(actor.userInfo, EconomyRuntime.config.reSignTime)
        return if (signable) {
            ActionResult.Continue
        } else {
            // 文案与原实现保持一致，避免玩家侧感知到行为变化。
            ActionResult.EndWithMessage("你已经签到过了哦!")
        }
    }

    /**
     * 前置规则 2（order=20）：计算本次签到奖励。
     *
     * 拆成规则而不是留在动作里，是为了让「奖励加成」成为可插拔的扩展点：
     * 以后要加节日翻倍、公会加成，只需在本文件插入 order 在 20~30 之间的 `@Before`，
     * 读 [SignReward] 后调 `addMultiplier`，动作本体完全不用动。
     *
     * 计算分两步，都在 [SignService] 里（对内计算，不对外提供）：
     * 1. `randomSignGold`：按 0..1000 的随机权重抽金币档位与运气文案；
     * 2. `signProp`：称号倍率、签到月卡、双倍/三倍/补签卡，并登记待消耗道具。
     *
     * 注意：`signProp` 里的**补签卡会把 `oldSignNumber` 接回连签数**，属于状态修改。
     * 因此本条规则的 order 必须大于资格校验（10），保证「被拦截时不留下改了一半的状态」。
     *
     * 结果通过 `context.put` 交给动作本体，动作再 `context.get(SignReward::class)` 取出使用。
     *
     * @param context 本次调用上下文，用于传递计算好的 [SignReward]。
     * @param actor 本次签到的参与者快照。
     */
    @Before(actionId = "core.sign", order = 20)
    suspend fun calculateReward(
        context: ActionContext,
        @FromProvider("signer") actor: SignActor,
    ) {
        val reward = SignReward(actor.userInfo)

        // 步骤 1：抽一个 0..1000 的权重，决定基础金币落在哪一档。
        reward.param = RandomUtil.randomInt(0, 1001)

        // 步骤 2：准备「本次签到触发事件:」这行标题；后面称号/道具的文案都追加在它下面。
        reward.eventReplyAdd(MessageUtil.formatMessageChain(actor.message, "本次签到触发事件:"))

        // 步骤 3：抽基础金币档位（可能写入一条运气文案）。
        SignService.randomSignGold(reward)

        // 步骤 4：结算称号 / 月卡 / 双倍卡 / 三倍卡 / 补签卡带来的倍率与消耗。
        SignService.signProp(reward)

        // 步骤 5：把奖励载体放进上下文，供动作本体结算时读取。
        context.put(SignReward::class, reward)
    }

    // ==================================================================================
    // 二、core.sign 后置规则：签完之后做什么
    // ==================================================================================

    /**
     * 后置规则 1（order=10）：记录一条签到完成日志。
     *
     * 动作本体通过**返回值**交出 [SignOutcome] 快照，这里用 `context.actionResult` 读取。
     * 主流程提前结束（已签到、入账失败）时返回值为 null，本规则直接跳过，不抛异常。
     *
     * 后续要做签到统计、成就解锁、外部通知等后置逻辑，都在本文件追加 `@After`，
     * 不要再新增事件类或改 `economy-main` 的订阅。
     *
     * @param context 本次 `core.sign` 调用的上下文。
     */
    @After(actionId = "core.sign", order = 10)
    suspend fun logCommitted(context: ActionContext) {
        val outcome = context.actionResult(SignOutcome::class) ?: return
        Log.info(
            "签到完成:群(${outcome.groupId}) 用户(${outcome.qq}) " +
                "连签 ${outcome.signNumber} 天,本次收益 ${outcome.gold.toPlainString()}"
        )
    }

    /**
     * 后置规则 2（order=20）：签到成功后的称号检查。
     *
     * 原实现把这两步写在主流程中间（发图之前）。挪到 `@After` 的理由：
     * 它属于「已经结算完成才该发生」的副作用，一旦主流程因入账失败提前结束就不该打扰玩家；
     * 放在规则里也让「签到称号」这件事对 [SignUsecase] 完全透明。
     *
     * 仍以 `actionResult` 是否为空作为成功判据：空表示没签成，不检查称号。
     *
     * @param context 本次 `core.sign` 调用的上下文，用于判断签到是否真的成功。
     * @param actor 本次签到的参与者快照（称号检查要写 `userInfo`、要向群发通知）。
     */
    @After(actionId = "core.sign", order = 20)
    suspend fun grantSignTitles(
        context: ActionContext,
        @FromProvider("signer") actor: SignActor,
    ) {
        // 没签到成功就没有 SignOutcome，直接跳过称号检查。
        context.actionResult(SignOutcome::class) ?: return

        // 连续签到里程碑称号（签到狂人 / 签到大王）。
        TitleManager.checkSignTitle(actor.userInfo, actor.subject)

        // 首富类称号：金币变化后可能易主，顺手检查一次。
        TitleManager.checkMonopoly(actor.userInfo, actor.subject)
    }

    // ==================================================================================
    // 三、管理类动作的审计日志（core.sign.close / open / refresh）
    // ==================================================================================
    //
    // 这三个指令只是「改一个开关 / 改一个时间戳」，没有可扩展的业务阶段，
    // 因此规则链上只有一条审计日志。加日志是为了事后能回答「谁在哪个群关了签到」，
    // 不需要的话可以直接删掉，动作本体照样能跑（规则链允许某个动作零规则）。

    /**
     * `core.sign.close` 的后置审计：记录谁关闭了本群签到。
     *
     * 这里只读事件、不判断业务结果：关闭失败时也留痕，排查时反而更有用。
     *
     * @param context 本次调用的上下文，从中取触发事件。
     */
    @After(actionId = "core.sign.close", order = 10)
    suspend fun logClosed(context: ActionContext) {
        val event = context.event as? GroupMessageEvent ?: return
        Log.info("签到开关:群(${event.group.id}) 操作人(${event.sender.id}) 关闭签到")
    }

    /**
     * `core.sign.open` 的后置审计：记录谁重新开启了本群签到。
     *
     * @param context 本次调用的上下文，从中取触发事件。
     */
    @After(actionId = "core.sign.open", order = 10)
    suspend fun logOpened(context: ActionContext) {
        val event = context.event as? GroupMessageEvent ?: return
        Log.info("签到开关:群(${event.group.id}) 操作人(${event.sender.id}) 开启签到")
    }

    /**
     * `core.sign.refresh` 的后置审计：记录谁刷新了签到状态。
     *
     * 这属于管理员纠错操作（把 `signTime` 往前拨一天），必须留痕。
     *
     * @param context 本次调用的上下文，从中取触发事件。
     */
    @After(actionId = "core.sign.refresh", order = 10)
    suspend fun logRefreshed(context: ActionContext) {
        val event = context.event as? GroupMessageEvent ?: return
        Log.info("签到刷新:群(${event.group.id}) 操作人(${event.sender.id}) 刷新签到")
    }
}
