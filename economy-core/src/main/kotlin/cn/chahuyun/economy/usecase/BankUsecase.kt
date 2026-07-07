package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.manager.BankManager
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.user.user
import cn.chahuyun.economy.privatebank.PrivateBankService
import cn.chahuyun.economy.utils.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import xyz.cssxsh.mirai.economy.service.EconomyAccount

/**
 * 银行相关用例（主银行 + 默认私银路由）。
 */
object BankUsecase {

    private enum class BankRoute {
        DEFAULT,
        MAIN,
        PRIVATE
    }

    private data class BankTransferRequest(
        val amount: Double?,
        val route: BankRoute,
        val bankKey: String? = null
    )

    suspend fun deposit(event: MessageEvent) {
        val userInfo= UserCoreManager.getUserInfo(event.sender)
        val user = userInfo.user
        val subject: Contact = event.subject
        val request = parseBankTransfer(event.message.contentToString(), "存款", "deposit")
        if (request == null) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    "用法：存款! / 存款!! / 存款 <金额> [!/银行]"
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
            BankRoute.PRIVATE -> depositPrivateBank(event, userInfo, request.bankKey.orEmpty(), amount)
            BankRoute.DEFAULT -> {
                val defaultPb = userInfo.defaultPrivateBankCode.trim().takeIf { it.isNotBlank() }
                if (defaultPb.isNullOrBlank()) {
                    if (EconomyUtil.turnUserToBank(user, amount)) {
                        subject.sendMessage(MessageUtil.formatMessageChain(event.message, "存款成功：${MoneyFormatUtil.format(amount)} 已存入主银行"))
                    } else {
                        subject.sendMessage(MessageUtil.formatMessageChain(event.message, "存款失败!"))
                        Log.error("银行管理:默认主银行存款失败")
                    }
                } else {
                    depositPrivateBank(event, userInfo, defaultPb, amount)
                }
            }
        }
    }

    suspend fun withdrawal(event: MessageEvent) {
        val userInfo= UserCoreManager.getUserInfo(event.sender)
        val user = userInfo.user
        val subject: Contact = event.subject
        val request = parseBankTransfer(event.message.contentToString(), "取款", "withdraw")
        if (request == null) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    "用法：取款! / 取款!! / 取款 <金额> [!/银行]"
                )
            )
            return
        }

        when (request.route) {
            BankRoute.MAIN -> withdrawMainBank(event, request.amount ?: EconomyUtil.getMoneyByBank(user))
            BankRoute.PRIVATE -> withdrawPrivateBank(event, userInfo, request.bankKey.orEmpty(), request.amount)
            BankRoute.DEFAULT -> {
                val defaultPb = userInfo.defaultPrivateBankCode.trim().takeIf { it.isNotBlank() }
                if (defaultPb.isNullOrBlank()) {
                    withdrawMainBank(event, request.amount ?: EconomyUtil.getMoneyByBank(user))
                } else {
                    withdrawPrivateBank(event, userInfo, defaultPb, request.amount)
                }
            }
        }
    }

    private suspend fun depositPrivateBank(event: MessageEvent, userInfo: cn.chahuyun.economy.model.user.UserInfoDto, bankKey: String, amount: Double) {
        val (ok, msg) = PrivateBankService.deposit(userInfo.user, bankKey, amount)
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
        if (ok) {
            val bank = PrivateBankService.getBank(bankKey)
            if (bank != null) {
                userInfo.defaultPrivateBankCode = bank.code
                UserCoreManager.saveUserInfo(userInfo)
            }
        }
    }

    private suspend fun withdrawPrivateBank(event: MessageEvent, userInfo: cn.chahuyun.economy.model.user.UserInfoDto, bankKey: String, requestedAmount: Double?) {
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

        val (ok, msg) = PrivateBankService.withdraw(event.sender, bank.code, amount)
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
        if (ok) {
            userInfo.defaultPrivateBankCode = bank.code
            UserCoreManager.saveUserInfo(userInfo)
        }
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

    private fun parseBankTransfer(raw: String, chineseCommand: String, englishCommand: String): BankTransferRequest? {
        val text = raw.trim()
        if (text.isBlank()) return null
        val parts = text.split(Regex("\\s+"))
        val command = parts.firstOrNull() ?: return null
        val suffix = when {
            command.startsWith(chineseCommand) -> command.removePrefix(chineseCommand)
            command.startsWith(englishCommand) -> command.removePrefix(englishCommand)
            else -> return null
        }

        if (suffix == "!") return BankTransferRequest(null, BankRoute.DEFAULT)
        if (suffix == "!!") return BankTransferRequest(null, BankRoute.MAIN)
        if (suffix.isNotBlank()) return null

        val amountToken = parts.getOrNull(1) ?: return null
        if (amountToken == "!") return BankTransferRequest(null, BankRoute.DEFAULT)
        if (amountToken == "!!") return BankTransferRequest(null, BankRoute.MAIN)

        val amount = parseMoney(amountToken) ?: return null
        val target = parts.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }
        return when {
            target == null -> BankTransferRequest(amount, BankRoute.DEFAULT)
            target == "!" || target == "!!" || target == "主银行" || target.equals("main", ignoreCase = true) ->
                BankTransferRequest(amount, BankRoute.MAIN)
            else -> BankTransferRequest(amount, BankRoute.PRIVATE, target)
        }
    }

    private fun parseMoney(text: String): Double? {
        val normalized = text.trim()
        if (normalized.isBlank()) return null
        val match = Regex("""^(\d+(?:\.\d+)?)([kKmMgGtTpPwW万亿]?)$""").matchEntire(normalized) ?: return null
        val number = match.groupValues[1].toDoubleOrNull() ?: return null
        val multiplier = when (match.groupValues[2]) {
            "k", "K" -> 1_000.0
            "w", "W", "万" -> 10_000.0
            "m", "M" -> 1_000_000.0
            "g", "G", "亿" -> 100_000_000.0
            "t", "T" -> 1_000_000_000_000.0
            "p", "P" -> 1_000_000_000_000_000.0
            else -> 1.0
        }
        return number * multiplier
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

        val accountByBank: Map<EconomyAccount, Double> = EconomyUtil.getAccountByBank()
        val totalBankMoney = EconomyUtil.getBankTotalCached()

        // 全局银行可能存在同一用户的多个子账户（不同 description）。富豪榜需要按用户聚合，避免同一人多次上榜。
        val userTotals: List<Pair<String, Double>> = accountByBank.entries
            .groupBy({ it.key.uuid }, { it.value })
            .map { (uuid, values) -> uuid to values.sum() }
            .sortedByDescending { it.second }
            .take(10)

        var index = 1
        for ((uuid, money) in userTotals) {
            val userInfo = UserCoreManager.getUserInfo(uuid) ?: continue
            val group: Group? = bot.getGroup(userInfo.registerGroup)
            val groupName = group?.name ?: "未找到群"
            val groupDisplay = "${userInfo.registerGroup} (${groupName})"
            val ratio = if (totalBankMoney > 0) money / totalBankMoney * 100 else 0.0

            val plainText = MessageUtil.formatMessage(
                "top:${index++}\n" +
                    "用户:${userInfo.name.ifBlank { "未知" }}\n" +
                    "注册群:${groupDisplay}\n" +
                    "存款:${MoneyFormatUtil.format(money)}\n" +
                    "占比:${FormatUtil.fixed(ratio, 1)}%"
            )
            builder.add(bot, plainText)
        }

        subject.sendMessage(builder.build())
    }
}
