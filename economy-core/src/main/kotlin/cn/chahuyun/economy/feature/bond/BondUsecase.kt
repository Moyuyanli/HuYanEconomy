@file:Suppress("DuplicatedCode")

package cn.chahuyun.economy.feature.bond

import cn.chahuyun.economy.actionchain.annotation.Action
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.data.repository.PrivateBankRepository
import cn.chahuyun.economy.feature.bond.BondUsecase.PAGE_SIZE
import cn.chahuyun.economy.feature.privatebank.PrivateBankManager
import cn.chahuyun.economy.utils.FormatUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.MoneyFormatUtil
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import java.util.*

/**
 * 国卷（政府债）功能的主用例：查看发行、查看持仓、补发、购买、赎回。
 *
 * ## 动作清单
 * | actionId | 指令 | 规则链 |
 * |---|---|---|
 * | `core.bond.list` | 国卷 / 国卷列表 | `@After` 留痕 |
 * | `core.bond.holdings` | 国卷持仓 | `@Before(10)` 行长身份；`@After`  |
 * | `core.bond.supplement` | 国卷补发（owner） | `@After` 留痕 |
 * | `core.bond.buy` | 国卷购买 金额 code | `@Before(10)` 参数 + 行长银行；`@After` 留痕 |
 * | `core.bond.redeem` | 国卷赎回 code [金额] | `@Before(10)` 同上；`@After` 留痕 |
 *
 * ## 与私银的分工
 * 狐卷（竞标债）属于私银模块，入口留在 `PrivateBankUsecase`，避免同一条狐卷消息被两个监听器重复响应。
 * 本类原为 `usecase/FoxBondUsecase`，其中 `foxView` / `foxBid` / `foxSupplement` 三个方法与
 * 私银的狐卷实现完全重复且无调用方，迁移时删除。
 */
object BondUsecase {

    /** 转发分页大小：每个转发节点最多 15 张国卷。 */
    private const val PAGE_SIZE = 15

    private const val DAY_MILLIS = 86_400_000L

    /**
     * 查看仍可购买的国卷（`core.bond.list`）。
     *
     * @param event 消息事件（取 bot 构造转发）。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.bond.list")
    suspend fun bondList(event: MessageEvent, context: ActionContext) {
        Log.info("国卷指令:列表 用户(${event.sender.id})")

        val actor = context.provide(BondActor::class, BondProvider.ACTOR)
        PrivateBankManager.ensureDailyBondIssues()
        val issues = PrivateBankManager.listAvailableBondIssues()
        if (issues.isEmpty()) {
            actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "当前没有可购买的国卷"))
            return
        }

        val lines = issues.map { issue ->
            val code = issue.code.ifBlank { issue.weekKey }
            buildString {
                append(code)
                append("\n剩余：${MoneyFormatUtil.format(issue.remaining)} / ${MoneyFormatUtil.format(issue.totalLimit)}")
                append("\n利率：${FormatUtil.fixed(issue.rateMultiplier, 2)}%/day")
                append("\n锁仓：${issue.lockDays} 天")
                append("\n赎回时间：${DateUtil.formatDateTime(Date(PrivateBankManager.bondRedeemAt(issue)))}")
            }
        }
        sendForwardPages(actor, "可购买国卷", lines)
    }

    /**
     * 查看当前发送者作为行长所拥有银行的国卷持仓（`core.bond.holdings`）。
     *
     * @param event 消息事件（取 bot 构造转发）。
     * @param context 本次调用上下文，行长银行由规则放入 [BondHoldingScope]。
     */
    @Action("core.bond.holdings")
    suspend fun bondHoldings(event: MessageEvent, context: ActionContext) {
        Log.info("国卷指令:持仓 用户(${event.sender.id})")

        val actor = context.provide(BondActor::class, BondProvider.ACTOR)
        val scope = context.get(BondHoldingScope::class)
        if (scope == null) {
            Log.warning("国卷:持仓规则未产出范围，本次终止（用户 ${event.sender.id}）")
            return
        }

        val issueById = PrivateBankRepository.listBondIssues().associateBy { it.id }
        val holdings = PrivateBankRepository.listBondHoldings(scope.bankCode)
            .filter { it.redeemedAt == 0L && it.principal > 0.0001 }
            .sortedBy { it.boughtAt + it.lockDays * DAY_MILLIS }
        if (holdings.isEmpty()) {
            actor.subject.sendMessage(
                MessageUtil.formatMessageChain(actor.message, "${scope.bankName} 暂无国卷持仓")
            )
            return
        }

        val now = System.currentTimeMillis()
        val lines = holdings.map { holding ->
            val issue = issueById[holding.issueId]
            val code = issue?.code?.ifBlank { issue.weekKey } ?: "#${holding.issueId}"
            val dueAt = holding.boughtAt + holding.lockDays * DAY_MILLIS
            buildString {
                append(code)
                append("\n本金：${MoneyFormatUtil.format(holding.principal)}")
                append("\n利率：${FormatUtil.fixed(holding.rateMultiplier, 2)}%/day")
                append("\n购买时间：${DateUtil.formatDateTime(Date(holding.boughtAt))}")
                append("\n赎回时间：${DateUtil.formatDateTime(Date(dueAt))}")
                append("\n状态：${if (dueAt <= now) "可赎回" else "锁仓中"}")
            }
        }
        sendForwardPages(actor, "${scope.bankName} 国卷持仓", lines)
    }

    /**
     * 补发当日国卷（`core.bond.supplement`，入口已限 owner 权限）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.bond.supplement")
    suspend fun supplementBonds(event: MessageEvent, context: ActionContext) {
        Log.info("国卷指令:补发 用户(${event.sender.id})")

        val actor = context.provide(BondActor::class, BondProvider.ACTOR)
        val issues = PrivateBankManager.supplementDailyBondIssues()
        actor.subject.sendMessage(
            MessageUtil.formatMessageChain(actor.message, "国卷补发完成，本次新增 ${issues.size} 个国卷")
        )
    }

    /**
     * 购买国卷：行长用流动金池资金购买指定 code 的国卷（`core.bond.buy`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [BondBuyPlan] 与参与者快照。
     */
    @Action("core.bond.buy")
    suspend fun buyBond(event: MessageEvent, context: ActionContext) {
        Log.info("国卷指令:购买 用户(${event.sender.id})")

        val actor = context.provide(BondActor::class, BondProvider.ACTOR)
        val plan = context.get(BondBuyPlan::class)
        if (plan == null) {
            Log.warning("国卷:购买规则未产出计划，本次终止（用户 ${event.sender.id}）")
            return
        }

        val (_, msg) = PrivateBankManager.buyBond(actor.sender, plan.bankCode, plan.code, plan.amount)
        actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }

    /**
     * 赎回国卷：不带金额时赎回该 code 的全部持仓，带金额时赎回指定本金（`core.bond.redeem`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [BondRedeemPlan] 与参与者快照。
     */
    @Action("core.bond.redeem")
    suspend fun redeemBond(event: MessageEvent, context: ActionContext) {
        Log.info("国卷指令:赎回 用户(${event.sender.id})")

        val actor = context.provide(BondActor::class, BondProvider.ACTOR)
        val plan = context.get(BondRedeemPlan::class)
        if (plan == null) {
            Log.warning("国卷:赎回规则未产出计划，本次终止（用户 ${event.sender.id}）")
            return
        }

        val (_, msg) = PrivateBankManager.redeemBond(actor.sender, plan.code, plan.amount)
        actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
    }

    /**
     * 按 [PAGE_SIZE] 分页发转发消息。
     *
     * @param actor 本次指令的参与者快照（提供发送目标）。
     * @param title 每页标题前缀。
     * @param lines 每条记录的多行文本。
     */
    private suspend fun sendForwardPages(actor: BondActor, title: String, lines: List<String>) {
        val pages = lines.chunked(PAGE_SIZE)
        val subject: Contact = actor.subject
        pages.forEachIndexed { pageIndex, page ->
            val builder = ForwardMessageBuilder(subject)
            builder.add(
                subject.bot,
                PlainText("$title ${pageIndex + 1}/${pages.size}（共 ${lines.size} 个）")
            )
            page.forEach { builder.add(subject.bot, PlainText(it)) }
            subject.sendMessage(builder.build())
        }
    }
}
