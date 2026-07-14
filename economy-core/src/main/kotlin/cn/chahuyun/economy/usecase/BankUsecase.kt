package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.manager.BankManager
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.model.user.user
import cn.chahuyun.economy.privatebank.PrivateBankService
import cn.chahuyun.economy.service.EconomyAsyncService
import cn.chahuyun.economy.utils.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import xyz.cssxsh.mirai.economy.service.EconomyAccount
import java.util.*

/**
 * 银行相关用例（主银行 + 默认私银路由）。
 */
object BankUsecase {
    private const val REGAL_TOP_CACHE_TTL_MS = 5 * 60 * 1000L

    @Volatile
    private var regalTopCache: RegalTopCache? = null
    private val regalTopCacheMutex = Mutex()

    suspend fun deposit(event: MessageEvent) {
        val userInfo= UserCoreManager.getUserInfo(event.sender)
        val user = userInfo.user
        val subject: Contact = event.subject
        val request = BankTransferParser.parse(event.message.contentToString(), "存款", "deposit")
        if (request == null) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    "用法：存款! / 存款!! / 存款 <金额> [银行|main]"
                )
            )
            return
        }

        val amount = request.amount ?: EconomyUtil.getMoneyByUser(user)
        if (amount <= 0) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你的钱包没有可存入的金币"))
            return
        }

        val wallet = EconomyUtil.getMoneyByUser(user)
        if (wallet + 1e-6 < amount) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你的金币不够 ${MoneyFormatUtil.format(amount)}"))
            return
        }

        when (request.route) {
            BankRoute.MAIN -> {
                if (EconomyUtil.turnUserToBank(user, amount)) {
                    subject.sendMessage(MessageUtil.formatMessageChain(event.message, "存款成功：${MoneyFormatUtil.format(amount)} 已存入主银行"))
                } else {
                    subject.sendMessage(MessageUtil.formatMessageChain(event.message, "存款失败!"))
                    Log.error("银行管理:主银行存款失败")
                }
            }
            BankRoute.PRIVATE -> depositPrivateBank(event, user, request.bankKey.orEmpty(), amount)
            BankRoute.DEFAULT -> {
                val defaultPb = defaultPrivateBankKey(userInfo.defaultPrivateBankCode)
                if (defaultPb == null) {
                    if (EconomyUtil.turnUserToBank(user, amount)) {
                        subject.sendMessage(MessageUtil.formatMessageChain(event.message, "存款成功：${MoneyFormatUtil.format(amount)} 已存入主银行"))
                    } else {
                        subject.sendMessage(MessageUtil.formatMessageChain(event.message, "存款失败!"))
                        Log.error("银行管理:默认主银行存款失败")
                    }
                } else {
                    depositPrivateBank(event, user, defaultPb, amount)
                }
            }
        }
    }

    suspend fun withdrawal(event: MessageEvent) {
        val userInfo= UserCoreManager.getUserInfo(event.sender)
        val user = userInfo.user
        val subject: Contact = event.subject
        val request = BankTransferParser.parse(event.message.contentToString(), "取款", "withdraw")
        if (request == null) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    "用法：取款! / 取款!! / 取款 <金额> [银行|main]"
                )
            )
            return
        }

        when (request.route) {
            BankRoute.MAIN -> withdrawMainBank(event, request.amount ?: EconomyUtil.getMoneyByBank(user))
            BankRoute.PRIVATE -> withdrawPrivateBank(event, request.bankKey.orEmpty(), request.amount)
            BankRoute.DEFAULT -> {
                val defaultPb = defaultPrivateBankKey(userInfo.defaultPrivateBankCode)
                if (defaultPb == null) {
                    withdrawMainBank(event, request.amount ?: EconomyUtil.getMoneyByBank(user))
                } else {
                    withdrawPrivateBank(event, defaultPb, request.amount)
                }
            }
        }
    }

    internal fun defaultPrivateBankKey(raw: String?): String? {
        val key = raw?.trim().orEmpty()
        return key.takeUnless {
            it.isBlank() || it.equals("main", ignoreCase = true) || it == "主银行"
        }
    }

    private suspend fun depositPrivateBank(event: MessageEvent, user: User, bankKey: String, amount: Double) {
        val (_, msg) = PrivateBankService.deposit(user, bankKey, amount)
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    private suspend fun withdrawPrivateBank(event: MessageEvent, bankKey: String, requestedAmount: Double?) {
        val bank = PrivateBankService.getBank(bankKey)
        if (bank == null) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "未找到该银行：$bankKey"))
            return
        }
        val amount = requestedAmount ?: (PrivateBankService.getDeposit(bank.code, event.sender.id)?.principal ?: 0.0)
        if (amount <= 0) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你在该银行没有可取出的存款"))
            return
        }

        val (_, msg) = PrivateBankService.withdraw(event.sender, bank.code, amount)
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    private suspend fun withdrawMainBank(event: MessageEvent, amount: Double) {
        val user = event.sender
        val subject: Contact = event.subject
        if (amount <= 0) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你的主银行没有可取出的金币"))
            return
        }

        val moneyByBank = EconomyUtil.getMoneyByBank(user)
        if (moneyByBank + 1e-6 < amount) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你的银行余额不够 ${MoneyFormatUtil.format(amount)} 枚金币"))
            return
        }

        if (EconomyUtil.turnBankToUser(user, amount)) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "取款成功：${MoneyFormatUtil.format(amount)} 已从主银行取出"))
        } else {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "取款失败!"))
            Log.error("银行管理:主银行取款失败")
        }
    }

    suspend fun viewBankInterest(event: MessageEvent) {
        Log.info("银行指令")

        val bankInfo = BankManager.getBankInfo(1)
        if (bankInfo == null) {
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "银行信息未初始化"))
            return
        }
        val weeklyRate = bankInfo.interest / 10.0
        event.subject.sendMessage(
            MessageUtil.formatMessageChain(
                event.message,
                "本周银行利率是${FormatUtil.fixed(weeklyRate, 1)}%"
            )
        )
    }

    suspend fun viewRegalTop(event: MessageEvent) {
        Log.info("经济指令")

        val subject = event.subject
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
        val accountByBank: Map<EconomyAccount, Double> = EconomyUtil.getAccountByBank()
        val accountBalances = HashMap<String, Double>(accountByBank.size)
        accountByBank.forEach { (account, money) ->
            accountBalances.merge(account.uuid, money, Double::plus)
        }

        val snapshot = buildRegalTopSnapshot(
            accountBalances = accountBalances,
            users = UserCoreManager.listRankingUsers()
        )
        val userTotals = snapshot.entries
        val totalRankedMoney = snapshot.totalMoney

        var index = 1
        return userTotals.map { entry ->
            val userInfo = entry.userInfo
            val group: Group? = bot.getGroup(userInfo.registerGroup)
            val groupName = group?.name ?: "未找到群"
            val groupDisplay = "${userInfo.registerGroup} (${groupName})"
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
