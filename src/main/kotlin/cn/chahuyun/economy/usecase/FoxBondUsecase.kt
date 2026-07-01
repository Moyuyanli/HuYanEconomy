package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.privatebank.PrivateBankFoxBondService
import cn.chahuyun.economy.privatebank.PrivateBankRepository
import cn.chahuyun.economy.privatebank.PrivateBankService
import cn.chahuyun.economy.utils.FormatUtil
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.MoneyFormatUtil
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 鐙愬嵎/鍥藉嵎鐢ㄤ緥锛堝吋瀹逛袱涓叧閿瘝锛夈€?
 */
object FoxBondUsecase {

    /**
     * 鏌ョ湅褰撳墠鍙珵鏍囩殑鐙愬嵎鍒楄〃
     */
    suspend fun foxView(event: MessageEvent) {
        val subject: Contact = event.subject
        val bonds = PrivateBankFoxBondService.listActiveBonds()
        if (bonds.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "褰撳墠娌℃湁鍙珵鏍囩殑鐙愬嵎"))
            return
        }

        val msg = buildString {
            append("褰撳墠鍙珵鏍囩嫄鍗凤紙鏈€澶氬睍绀?10 鏉★級\n")
            bonds.take(10).forEach { b ->
                append(
                    "${b.code} | 闈㈤=${MoneyFormatUtil.format(b.faceValue)} | 鍘熷=${
                        FormatUtil.fixed(b.baseRate, 2)
                    }%/day | 鏈熼檺=${b.termDays}澶?| 鎴=${DateUtil.formatDateTime(java.util.Date(b.bidEndAt))}\n"
                )
            }
            append("鐢ㄦ硶锛氱嫄鍗风珵鏍?<code> <婧环閲戦> <鎺ュ彈鍒╂伅(%/day)>\n")
            append("绀轰緥锛氱嫄鍗风珵鏍?")
            append(bonds.first().code)
            append(" 5000000 3.2")
        }
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg.trimEnd()))
    }

    /**
     * 鎻愪氦鐙愬嵎绔炴爣
     */
    suspend fun foxBid(event: MessageEvent) {
        val subject: Contact = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        if (parts.size < 4) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    "鐢ㄦ硶锛氱嫄鍗风珵鏍?<code> <婧环閲戦> <鎺ュ彈鍒╂伅(%/day)>"
                )
            )
            return
        }
        val code = parts[1]
        val premium = parts[2].toDoubleOrNull() ?: 0.0
        val rate = parts[3].toDoubleOrNull() ?: 0.0
        val (_, msg) = PrivateBankFoxBondService.submitBid(event.sender, code, premium, rate)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    /**
     * 璐拱鍥藉嵎锛氳闀跨敤娴佸姩閲戞睜璧勯噾璐拱鏈懆鍥藉嵎
     */
    suspend fun buyBond(event: MessageEvent) {
        val subject: Contact = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        val amount = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        if (amount <= 0) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "鐢ㄦ硶锛氬浗鍗疯喘涔?<閲戦>"))
            return
        }

        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == event.sender.id }
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你还没有创建自己的银行"))
            return
        }

        val (ok, msg) = PrivateBankService.buyBond(event.sender, bank.code, amount)
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    /**
     * 璧庡洖鍥藉嵎锛氫笉甯D鍒欒祹鍥炲叏閮ㄥ埌鏈熸寔浠擄紝甯D鍒欒祹鍥炴寚瀹氭寔浠?
     */
    suspend fun redeemBond(event: MessageEvent) {
        val subject: Contact = event.subject
        val parts = event.message.contentToString().trim().split(" ")
        val holdingId = parts.getOrNull(1)?.toIntOrNull()

        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == event.sender.id }
        if (bank == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你还没有创建自己的银行"))
            return
        }

        if (holdingId != null) {
            // 璧庡洖鎸囧畾鎸佷粨
            val (ok, msg) = PrivateBankService.redeemBond(event.sender, holdingId)
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
        } else {
            // 璧庡洖鍏ㄩ儴鍒版湡鎸佷粨
            val holdings = PrivateBankRepository.listBondHoldings(bank.code)
                .filter { it.redeemedAt == 0L }

            if (holdings.isEmpty()) {
                subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你没有国债持仓"))
                return
            }

            var successCount = 0
            var totalPayout = 0.0
            val results = mutableListOf<String>()

            for (h in holdings) {
                val (ok, msg) = PrivateBankService.redeemBond(event.sender, h.id)
                if (ok) {
                    successCount++
                    results.add("鎸佷粨#${h.id}: $msg")
                } else {
                    results.add("鎸佷粨#${h.id}: $msg")
                }
            }

            val summary = buildString {
                append("鍥藉嵎璧庡洖缁撴灉锛堝叡 ${holdings.size} 绗旓級\n")
                results.forEach { append("$it\n") }
            }
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, summary.trimEnd()))
        }
    }

    /**
     * 鏌ョ湅鏈懆鍥藉嵎鍙戣淇℃伅 + 鏈鎸佷粨鍒楄〃
     */
    suspend fun bondList(event: MessageEvent) {
        val subject: Contact = event.subject
        val issue = PrivateBankService.ensureWeeklyBondIssue()

        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == event.sender.id }

        val msg = buildString {
            append("鏈懆鍥藉嵎淇℃伅\n")
            append("鏈熷彿: ${issue.weekKey}\n")
            append("鍒╃巼鍊嶆暟: ${FormatUtil.fixed(issue.rateMultiplier, 2)}x\n")
            append("閿佷粨澶╂暟: ${issue.lockDays} 澶‐n")
            append("鎬婚搴? ${MoneyFormatUtil.format(issue.totalLimit)}\n")
            append("鍓╀綑棰濆害: ${MoneyFormatUtil.format(issue.remaining)}\n")

            if (bank != null) {
                val holdings = PrivateBankRepository.listBondHoldings(bank.code)
                    .filter { it.redeemedAt == 0L }
                if (holdings.isNotEmpty()) {
                    append("\n浣犵殑閾惰鎸佷粨锛?{bank.name}锛塡n")
                    holdings.forEach { h ->
                        val dueAt = java.util.Date(h.boughtAt + h.lockDays * 86400000L)
                        val isExpired = dueAt.before(java.util.Date())
                        val status = if (isExpired) "已到期" else "未到期"
                        append("  #${h.id} | 金额=${MoneyFormatUtil.format(h.principal)} | ${h.rateMultiplier}x | $status\n")
                    }
                } else {
                    append("\n浣犵殑閾惰鏆傛棤鍥藉嵎鎸佷粨\n")
                }
                append("\n鐢ㄦ硶锛氬浗鍗疯喘涔?<閲戦> | 鍥藉嵎璧庡洖 [鎸佷粨ID]")
            } else {
                append("\n你还没有创建银行，无法购买国债")
            }
        }
        subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg.trimEnd()))
    }
}
