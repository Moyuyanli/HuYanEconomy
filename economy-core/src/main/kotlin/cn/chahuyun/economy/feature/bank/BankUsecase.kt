package cn.chahuyun.economy.feature.bank

import cn.chahuyun.economy.actionchain.annotation.Action
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.feature.privatebank.PrivateBankManager
import cn.chahuyun.economy.feature.user.UserManager
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.service.EconomyAsyncService
import cn.chahuyun.economy.utils.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import java.util.*

/**
 * 主银行功能的主用例：存取款（含默认私银路由）、利率查询、富豪榜。
 *
 * ## 动作清单
 * | actionId | 指令 | 规则链 |
 * |---|---|---|
 * | `core.bank.deposit` | 存款! / 存款!! / 存款 金额 [银行\|main] | `@Before(10)` 路由与金额校验；`@After` 留痕 |
 * | `core.bank.withdraw` | 取款! / 取款!! / 取款 金额 [银行\|main] | `@Before(10)` 同上（含主银行余额校验）；`@After` 留痕 |
 * | `core.bank.rate` | 本周利率 / 银行利率 | `@After` 留痕 |
 * | `core.bank.regalTop` | 富豪榜 / 经济排行 | `@After` 留痕 |
 *
 * 「默认银行」的路由归一化在 [BankRules] 里完成，动作拿到的 [BankDepositPlan] / [BankWithdrawPlan]
 * 已经是「主银行」或「某家具体私银」，因此这里不再判断第三态。
 */
object BankUsecase {
    private const val REGAL_TOP_CACHE_TTL_MS = 5 * 60 * 1000L

    @Volatile
    private var regalTopCache: RegalTopCache? = null
    private val regalTopCacheMutex = Mutex()

    /**
     * 存款（`core.bank.deposit`）。
     *
     * 金额、余额与目标账户都已由 [BankRules.validateDeposit] 确认，这里只做转移。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [BankDepositPlan] 与参与者快照。
     */
    @Action("core.bank.deposit")
    suspend fun deposit(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:存款 用户(${event.sender.id})")

        val actor = context.provide(BankActor::class, BankProvider.ACTOR)
        val plan = context.get(BankDepositPlan::class)
        if (plan == null) {
            Log.warning("银行管理:存款规则未产出计划，本次终止（用户 ${actor.user.id}）")
            return
        }

        when (plan.route) {
            BankRoute.MAIN -> {
                if (EconomyUtil.turnUserToBank(actor.user, plan.amount)) {
                    actor.subject.sendMessage(
                        MessageUtil.formatMessageChain(
                            actor.message,
                            "存款成功：${MoneyFormatUtil.format(plan.amount)} 已存入主银行"
                        )
                    )
                } else {
                    actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "存款失败!"))
                    Log.error("银行管理:主银行存款失败")
                }
            }
            BankRoute.PRIVATE -> {
                val (_, msg) = PrivateBankManager.deposit(actor.user, plan.bankKey.orEmpty(), plan.amount)
                actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
            }
            BankRoute.DEFAULT -> Log.error("银行管理:存款路由未归一化（用户 ${actor.user.id}）")
        }
    }

    /**
     * 取款（`core.bank.withdraw`）。
     *
     * 主银行路由的金额与余额已在规则里校验；私银路由的「不填金额 = 取全额」在规则里保留为空值。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [BankWithdrawPlan] 与参与者快照。
     */
    @Action("core.bank.withdraw")
    suspend fun withdrawal(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:取款 用户(${event.sender.id})")

        val actor = context.provide(BankActor::class, BankProvider.ACTOR)
        val plan = context.get(BankWithdrawPlan::class)
        if (plan == null) {
            Log.warning("银行管理:取款规则未产出计划，本次终止（用户 ${actor.user.id}）")
            return
        }

        when (plan.route) {
            BankRoute.MAIN -> {
                val amount = plan.amount ?: 0.0
                if (EconomyUtil.turnBankToUser(actor.user, amount)) {
                    actor.subject.sendMessage(
                        MessageUtil.formatMessageChain(
                            actor.message,
                            "取款成功：${MoneyFormatUtil.format(amount)} 已从主银行取出"
                        )
                    )
                } else {
                    actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "取款失败!"))
                    Log.error("银行管理:主银行取款失败")
                }
            }
            BankRoute.PRIVATE -> {
                val bankKey = plan.bankKey.orEmpty()
                val amount = plan.amount
                    ?: (PrivateBankManager.getDeposit(bankKey, actor.user.id)?.principal ?: 0.0)
                if (amount <= 0) {
                    actor.subject.sendMessage(
                        MessageUtil.formatMessageChain(actor.message, "你在该银行没有可取出的存款")
                    )
                    return
                }
                val (_, msg) = PrivateBankManager.withdraw(actor.user, bankKey, amount)
                actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, msg))
            }
            BankRoute.DEFAULT -> Log.error("银行管理:取款路由未归一化（用户 ${actor.user.id}）")
        }
    }

    /**
     * 查看本周主银行活期利率（`core.bank.rate`）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.bank.rate")
    suspend fun viewBankInterest(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:利率 用户(${event.sender.id})")

        val actor = context.provide(BankActor::class, BankProvider.ACTOR)
        val bankInfo = BankManager.getBankInfo(1)
        if (bankInfo == null) {
            actor.subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "银行信息未初始化"))
            return
        }
        val weeklyRate = bankInfo.interest / 10.0
        actor.subject.sendMessage(
            MessageUtil.formatMessageChain(
                actor.message,
                "本周银行利率是${FormatUtil.fixed(weeklyRate, 1)}%"
            )
        )
    }

    /**
     * 查看主银行存款排行榜（`core.bank.regalTop`）。
     *
     * @param event 消息事件（取 bot 实例）。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.bank.regalTop")
    suspend fun viewRegalTop(event: MessageEvent, context: ActionContext) {
        Log.info("银行指令:富豪榜 用户(${event.sender.id})")

        val actor = context.provide(BankActor::class, BankProvider.ACTOR)
        val subject: Contact = actor.subject
        val bot: Bot = event.bot

        val builder = ForwardMessageBuilder(subject)
        builder.add(bot, PlainText("以下是银行存款排行榜:"))

        val lines = getRegalTopLines(bot)
        if (lines.isEmpty()) {
            builder.add(bot, PlainText("暂无银行存款排行数据"))
        } else {
            for (line in lines) {
                builder.add(bot, MessageUtil.formatMessage(line))
            }
        }

        subject.sendMessage(builder.build())
    }

    /**
     * 把用户资料里的默认私银 code 归一化成「可用的私银 code」。
     *
     * 主银行用 `""` / `main` / `主银行` 三种写法表示「没有默认私银」。
     *
     * @param raw 用户配置的默认私银 code。
     * @return 具体私银 code；返回 `null` 表示该走主银行。
     */
    internal fun defaultPrivateBankKey(raw: String?): String? {
        val key = raw?.trim().orEmpty()
        return key.takeUnless {
            it.isBlank() || it.equals("main", ignoreCase = true) || it == "主银行"
        }
    }

    private suspend fun getRegalTopLines(bot: Bot, now: Long = System.currentTimeMillis()): List<String> {
        val cached = regalTopCache
        if (cached != null && now - cached.createdAt < REGAL_TOP_CACHE_TTL_MS) {
            return cached.lines
        }

        return regalTopCacheMutex.withLock {
            val latest = regalTopCache
            if (latest != null && now - latest.createdAt < REGAL_TOP_CACHE_TTL_MS) {
                latest.lines
            } else {
                withContext(EconomyAsyncService.coroutineDispatcher()) {
                    buildRegalTopLines(bot)
                }.also { lines ->
                    regalTopCache = RegalTopCache(now, lines)
                }
            }
        }
    }

    private fun buildRegalTopLines(bot: Bot): List<String> {
        val bankEntries = EconomyUtil.bankEntries()
        val accountBalances = HashMap<String, Double>(bankEntries.size)
        bankEntries.forEach { entry ->
            accountBalances.merge(entry.accountId, entry.amount.toDouble(), Double::plus)
        }

        val snapshot = buildRegalTopSnapshot(
            accountBalances = accountBalances,
            users = UserManager.listRankingUsers()
        )
        val userTotals = snapshot.entries
        val totalRankedMoney = snapshot.totalMoney

        var index = 1
        return userTotals.map { entry ->
            val userInfo = entry.userInfo
            val group: Group? = bot.getGroup(userInfo.registerGroup)
            val groupName = group?.name ?: "未找到群"
            val groupDisplay = "${userInfo.registerGroup} ($groupName)"
            val ratio = if (totalRankedMoney > 0) entry.money / totalRankedMoney * 100 else 0.0

            "top:${index++}\n" +
                "用户:${userInfo.name.ifBlank { "未知" }}\n" +
                "注册群:${groupDisplay}\n" +
                "存款:${MoneyFormatUtil.format(entry.money)}\n" +
                "占比:${FormatUtil.fixed(ratio, 1)}%"
        }
    }

    private data class RegalTopCache(
        val createdAt: Long,
        val lines: List<String>,
    )

    internal data class RegalTopEntry(
        val userInfo: UserInfoResolved,
        val money: Double,
    )

    internal data class RegalTopSnapshot(
        val entries: List<RegalTopEntry>,
        val totalMoney: Double,
    )

    internal data class UserInfoResolved(
        val qq: Long,
        val name: String,
        val registerGroup: Long,
    )

    internal fun buildRegalTopEntries(
        accountBalances: Map<String, Double>,
        users: List<UserInfoDto>,
        limit: Int = 10,
    ): List<RegalTopEntry> =
        buildRegalTopSnapshot(accountBalances, users, limit).entries

    internal fun buildRegalTopSnapshot(
        accountBalances: Map<String, Double>,
        users: List<UserInfoDto>,
        limit: Int = 10,
    ): RegalTopSnapshot {
        val lookup = RankingUserLookup(users)
        val totals = HashMap<Long, RegalTopEntry>()

        accountBalances.forEach { (uuid, money) ->
            if (uuid.isBlank() || money <= 0.0) return@forEach
            val userInfo = lookup.resolve(uuid) ?: return@forEach
            val current = totals[userInfo.qq]
            totals[userInfo.qq] = if (current == null) {
                RegalTopEntry(userInfo, money)
            } else {
                RegalTopEntry(current.userInfo, current.money + money)
            }
        }

        return RegalTopSnapshot(
            entries = topRegalEntries(totals.values, limit),
            totalMoney = totals.values.sumOf { it.money }
        )
    }

    private fun topRegalEntries(entries: Collection<RegalTopEntry>, limit: Int): List<RegalTopEntry> {
        if (limit <= 0) return emptyList()

        val heap = PriorityQueue<RegalTopEntry>(compareBy { it.money })
        entries.forEach { entry ->
            if (entry.money <= 0.0) return@forEach
            if (heap.size < limit) {
                heap.add(entry)
            } else if (entry.money > heap.peek().money) {
                heap.poll()
                heap.add(entry)
            }
        }

        return heap.toList().sortedByDescending { it.money }
    }

    private class RankingUserLookup(users: List<UserInfoDto>) {
        private val byId = HashMap<String, UserInfoResolved>()
        private val byFunding = HashMap<String, UserInfoResolved>()
        private val byQq = HashMap<String, UserInfoResolved>()

        init {
            users.forEach { user ->
                if (user.qq <= 0) return@forEach
                val resolved = UserInfoResolved(
                    qq = user.qq,
                    name = user.name,
                    registerGroup = user.registerGroup,
                )
                user.id.takeIf { it.isNotBlank() }?.let { byId.putIfAbsent(it, resolved) }
                user.funding.takeIf { it.isNotBlank() }?.let { byFunding.putIfAbsent(it, resolved) }
                byQq.putIfAbsent(user.qq.toString(), resolved)
            }
        }

        fun resolve(uuid: String): UserInfoResolved? =
            byId[uuid] ?: byFunding[uuid] ?: byQq[uuid]
    }
}
