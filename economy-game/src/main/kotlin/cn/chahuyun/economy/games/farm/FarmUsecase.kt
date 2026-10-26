package cn.chahuyun.economy.games.farm

import cn.chahuyun.economy.actionchain.annotation.Action
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.games.GamesReplySupport
import cn.chahuyun.economy.games.farm.FarmUsecase.view
import cn.chahuyun.economy.image.FarmDetailImageRenderer
import cn.chahuyun.economy.service.EconomyUserService
import cn.chahuyun.economy.utils.ImageMessageUtil
import cn.chahuyun.economy.utils.Log
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 农场玩法的主用例。
 *
 * ## 动作清单
 * | actionId | 指令 | 规则链 |
 * |---|---|---|
 * | `game.farm.view` | 我的农场 / 农场信息 | — |
 * | `game.farm.viewText` | 我的农场# / 农场信息# | — |
 * | `game.farm.detail` | 农场详情 | — |
 * | `game.farm.level` | 农场等级 | — |
 * | `game.farm.shop` | 农场商店 | — |
 * | `game.farm.warehouse` | 农场仓库 | — |
 * | `game.farm.blackMarket` | 黑市 | — |
 * | `game.farm.buySeed` | 购买种子 <作物> [数量] | — |
 * | `game.farm.plant` | 播种/种植 <土地…> <作物> | — |
 * | `game.farm.harvest` | 收获 <土地…> | — |
 * | `game.farm.sell` | 卖出果实 <作物\|全部> [数量] | — |
 * | `game.farm.upgrade` | 升级农场[*] | `@Before(10)` 解析逐级标记 |
 * | `game.farm.water` | 帮浇水 @用户 [*N] | `@Before(10)` 解析目标与次数 |
 * | `game.farm.steal` | 偷菜 @用户 | `@Before(10)` 解析目标 |
 * | `game.farm.sellAll` | 一键卖出 | — |
 * | `game.farm.harvestAll` | 一键收获 | — |
 * | `game.farm.plantAll` | 一键播种 <作物> | — |
 * | `game.farm.shield` | 激活守护 | — |
 *
 * 分工：参数解析、等级门槛、单/双用户并发锁与状态装载全在 [FarmCommandService]
 * （返回带失败文案的 [FarmOperationResult]）；命令层解析（@目标、浇水次数、逐级标记）
 * 在 [FarmRules]。本文件只做编排与回复。
 */
object FarmUsecase {

    // ---------------------------------------------------------------- 查看类

    /**
     * 查看农场（`game.farm.view`）：渲染成农场详情图片。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文。
     */
    @Action("game.farm.view")
    suspend fun view(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:我的农场 用户(${event.sender.id})")
        sendDetail(context.provide(FarmActor::class, FarmProvider.ACTOR))
    }

    /**
     * 查看农场纯文本版（`game.farm.viewText`，指令末尾带 `#`）。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文。
     */
    @Action("game.farm.viewText")
    suspend fun viewText(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:我的农场(文本) 用户(${event.sender.id})")
        val actor = context.provide(FarmActor::class, FarmProvider.ACTOR)
        replyView(actor) { FarmViewService.renderFarm(it) }
    }

    /**
     * 查看农场详情（`game.farm.detail`），与 [view] 同一张图片。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文。
     */
    @Action("game.farm.detail")
    suspend fun detail(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:农场详情 用户(${event.sender.id})")
        sendDetail(context.provide(FarmActor::class, FarmProvider.ACTOR))
    }

    /**
     * 查看农场等级（`game.farm.level`）。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文。
     */
    @Action("game.farm.level")
    suspend fun level(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:农场等级 用户(${event.sender.id})")
        val actor = context.provide(FarmActor::class, FarmProvider.ACTOR)
        replyView(actor) { FarmViewService.renderFarmLevel(it) }
    }

    /**
     * 查看农场商店（`game.farm.shop`）。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文。
     */
    @Action("game.farm.shop")
    suspend fun shop(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:农场商店 用户(${event.sender.id})")
        val actor = context.provide(FarmActor::class, FarmProvider.ACTOR)
        replyView(actor) { FarmViewService.renderShop(it) }
    }

    /**
     * 查看农场仓库（`game.farm.warehouse`）。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文。
     */
    @Action("game.farm.warehouse")
    suspend fun warehouse(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:农场仓库 用户(${event.sender.id})")
        val actor = context.provide(FarmActor::class, FarmProvider.ACTOR)
        replyView(actor) { FarmViewService.renderWarehouse(it) }
    }

    /**
     * 查看黑市（`game.farm.blackMarket`）。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文。
     */
    @Action("game.farm.blackMarket")
    suspend fun blackMarket(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:黑市 用户(${event.sender.id})")
        val actor = context.provide(FarmActor::class, FarmProvider.ACTOR)
        replyView(actor) { FarmViewService.blackMarketText(it) }
    }

    // ---------------------------------------------------------------- 经营类

    /**
     * 购买种子（`game.farm.buySeed`）：`购买种子 <作物> [数量]`。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文。
     */
    @Action("game.farm.buySeed")
    suspend fun buySeed(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:购买种子 用户(${event.sender.id})")
        val actor = context.provide(FarmActor::class, FarmProvider.ACTOR)
        val raw = FarmCommandTextParser.payload(actor.message.contentToString(), "购买种子")
        reply(actor, FarmCommandService.buySeed(actor.user, raw).message)
    }

    /**
     * 播种（`game.farm.plant`）：`播种|种植 <土地…> <作物>`。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文。
     */
    @Action("game.farm.plant")
    suspend fun plant(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:播种 用户(${event.sender.id})")
        val actor = context.provide(FarmActor::class, FarmProvider.ACTOR)
        val raw = FarmCommandTextParser.payload(actor.message.contentToString(), listOf("播种", "种植"))
        reply(actor, FarmCommandService.plant(actor.user, raw).message)
    }

    /**
     * 收获（`game.farm.harvest`）：`收获 <土地…>`。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文。
     */
    @Action("game.farm.harvest")
    suspend fun harvest(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:收获 用户(${event.sender.id})")
        val actor = context.provide(FarmActor::class, FarmProvider.ACTOR)
        val raw = FarmCommandTextParser.payload(actor.message.contentToString(), "收获")
        reply(actor, FarmCommandService.harvest(actor.user.id, raw).message)
    }

    /**
     * 卖出果实（`game.farm.sell`）：`卖出果实 <作物|全部> [数量]`。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文。
     */
    @Action("game.farm.sell")
    suspend fun sell(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:卖出果实 用户(${event.sender.id})")
        val actor = context.provide(FarmActor::class, FarmProvider.ACTOR)
        val raw = FarmCommandTextParser.payload(actor.message.contentToString(), "卖出果实")
        reply(actor, FarmCommandService.sellFruits(actor.user, raw).message)
    }

    /**
     * 升级农场（`game.farm.upgrade`）：`升级农场` 升一级，`升级农场*` 升到失败为止。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文，读取 [FarmUpgradeIntent]。
     */
    @Action("game.farm.upgrade")
    suspend fun upgrade(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:升级农场 用户(${event.sender.id})")
        val actor = context.provide(FarmActor::class, FarmProvider.ACTOR)
        val intent = context.get(FarmUpgradeIntent::class) ?: FarmUpgradeIntent(auto = false)
        val message = if (intent.auto) {
            FarmCommandService.upgradeFarmUntilFailure(actor.user)
        } else {
            FarmCommandService.upgradeFarm(actor.user).message
        }
        reply(actor, message)
    }

    /**
     * 帮浇水（`game.farm.water`）：`帮浇水 @用户 [*N]`。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文，读取 [FarmWaterIntent]。
     */
    @Action("game.farm.water")
    suspend fun water(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:帮浇水 用户(${event.sender.id})")
        val actor = context.provide(FarmActor::class, FarmProvider.ACTOR)
        val intent = context.get(FarmWaterIntent::class) ?: return
        val waterer = EconomyUserService.getOrCreate(actor.user)
        reply(actor, FarmCommandService.water(waterer, intent.targetQq, intent.times).message)
    }

    /**
     * 偷菜（`game.farm.steal`）：`偷菜 @用户`。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文，读取 [FarmTargetIntent]。
     */
    @Action("game.farm.steal")
    suspend fun steal(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:偷菜 用户(${event.sender.id})")
        val actor = context.provide(FarmActor::class, FarmProvider.ACTOR)
        val intent = context.get(FarmTargetIntent::class) ?: return
        val thief = EconomyUserService.getOrCreate(actor.user)
        reply(actor, FarmCommandService.steal(thief, intent.targetQq).message)
    }

    /**
     * 一键卖出（`game.farm.sellAll`，14 级开放）。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文。
     */
    @Action("game.farm.sellAll")
    suspend fun sellAll(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:一键卖出 用户(${event.sender.id})")
        val actor = context.provide(FarmActor::class, FarmProvider.ACTOR)
        reply(actor, FarmCommandService.sellAll(actor.user).message)
    }

    /**
     * 一键收获（`game.farm.harvestAll`，15 级开放）。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文。
     */
    @Action("game.farm.harvestAll")
    suspend fun harvestAll(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:一键收获 用户(${event.sender.id})")
        val actor = context.provide(FarmActor::class, FarmProvider.ACTOR)
        reply(actor, FarmCommandService.harvestAll(actor.user.id).message)
    }

    /**
     * 一键播种（`game.farm.plantAll`，16 级开放）：`一键播种 <作物>`。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文。
     */
    @Action("game.farm.plantAll")
    suspend fun plantAll(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:一键播种 用户(${event.sender.id})")
        val actor = context.provide(FarmActor::class, FarmProvider.ACTOR)
        val cropName = FarmCommandTextParser.payload(actor.message.contentToString(), "一键播种")
        reply(actor, FarmCommandService.plantAll(actor.user, cropName).message)
    }

    /**
     * 激活守护（`game.farm.shield`，17 级开放）。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文。
     */
    @Action("game.farm.shield")
    suspend fun shield(event: GroupMessageEvent, context: ActionContext) {
        Log.info("农场指令:激活守护 用户(${event.sender.id})")
        val actor = context.provide(FarmActor::class, FarmProvider.ACTOR)
        reply(actor, FarmCommandService.activateShield(actor.user).message)
    }

    // ---------------------------------------------------------------- 内部工具

    /** 引用原消息回复一行文本。 */
    private suspend fun reply(actor: FarmActor, message: String) {
        GamesReplySupport.reply(actor.subject, actor.message, message)
    }

    /**
     * 装载当前农场状态后渲染并回复。
     *
     * @param actor 本次指令的参与者快照。
     * @param render 由状态渲染成文本的渲染器。
     */
    private suspend fun replyView(actor: FarmActor, render: (FarmViewState) -> String) {
        val state = FarmViewService.getOrCreateViewState(actor.user.id)
        reply(actor, render(state))
    }

    /**
     * 发送农场详情图片；渲染或发送失败时降级为一句文本提示。
     *
     * @param actor 本次指令的参与者快照。
     */
    private suspend fun sendDetail(actor: FarmActor) {
        try {
            val card = FarmViewService.farmDetailCard(
                qq = actor.user.id,
                owner = "${actor.user.nameCardOrNick}(${actor.user.id})",
            )
            ImageMessageUtil.sendQuotedWebpImage(
                actor.subject,
                actor.message,
                FarmDetailImageRenderer.render(card),
            )
        } catch (e: Exception) {
            Log.error("农场详情图片生成或发送失败", e)
            reply(actor, "农场详情图片生成失败，请稍后再试。")
        }
    }
}
