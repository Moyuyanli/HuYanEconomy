package cn.chahuyun.economy.usecase

import cn.chahuyun.authorize.utils.AuthMessageUtil.sendMessageQuote
import cn.chahuyun.economy.data.PrizesData
import cn.chahuyun.economy.manager.LuckyDrawManager
import cn.chahuyun.economy.prizes.PrizeHandle
import cn.chahuyun.economy.prizes.PrizesUtil.draw
import cn.chahuyun.economy.prizes.PrizesUtil.drawTen
import cn.chahuyun.economy.prizes.RaffleContext
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent

object LuckyDrawUsecase {

    suspend fun luckyDraw(event: GroupMessageEvent) {
        val sender = event.sender
        val userId = sender.id
        val userRaffle = LuckyDrawManager.take(userId)
        val pool = PrizesData.pool.first()
        val group = event.group
        val context = RaffleContext(pool, userRaffle, group)

        // 1. 检查单抽冷却
        if (!LuckyDrawManager.checkSingleCooldown(userId)) {
            val remaining = LuckyDrawManager.singleRemainingSeconds(userId)
            event.sendMessageQuote("单抽冷却中，请再等 $remaining 秒")
            return
        }

        val price = pool.price
        if (EconomyUtil.getMoneyByUser(sender) < price.toDouble()) {
            event.sendMessageQuote("你的余额不足 $price ,无法抽奖")
            return
        }
        if (!EconomyUtil.minusMoneyToUser(sender, price.toDouble())) {
            event.sendMessageQuote("抽奖失败!")
            return
        }

        val result = pool.draw(context)
        val prize = result.prize

        group.sendMessage(
            MessageUtil.formatMessageChain(
                userId, """
            ${sender.nameCardOrNick} 本次抽奖结果: ${prize.name}
            ${prize.description}
        """.trimIndent()
            )
        )

        PrizeHandle.handle(listOf(result))
        LuckyDrawManager.updateCooldown(userId)
    }

    suspend fun luckyDrawTen(event: GroupMessageEvent) {
        val sender = event.sender
        val userId = sender.id
        val userRaffle = LuckyDrawManager.take(userId)
        val pool = PrizesData.pool.first()
        val group = event.group
        val context = RaffleContext(pool, userRaffle, group)

        // 1. 检查十连冷却
        if (!LuckyDrawManager.checkTenCooldown(userId)) {
            val remaining = LuckyDrawManager.tenRemainingSeconds(userId)
            event.sendMessageQuote("十连冷却中，请再等 $remaining 秒")
            return
        }

        val price = pool.price * 10.toDouble()
        if (EconomyUtil.getMoneyByUser(sender) < price) {
            event.sendMessageQuote("你的余额不足 $price ,无法抽奖")
            return
        }
        if (!EconomyUtil.minusMoneyToUser(sender, price)) {
            event.sendMessageQuote("抽奖失败!")
            return
        }

        val result = pool.drawTen(context)

        val random = group.members.random()
        val nick = sender.nameCardOrNick
        val message = MessageUtil.INSTANCE.buildForwardMessage(
            event, titleGenerator = "群聊的聊天记录", previewGenerator = listOf(
                "$nick:卧槽,中了!中了!!",
                "${event.bot.nameCardOrNick}:真的假滴?彩票滞销了?",
                "${random.nameCardOrNick}:我不信,除非你给我!"
            ), summarySize = 11
        ) {
            event.bot named event.bot.nameCardOrNick says "以下是你本次的10连结果↓:"
            result.forEach {
                val prize = it.prize
                sender.id named nick says """
                    ${prize.name}
                    ${prize.description}
                """.trimIndent()
            }
        }

        event.subject.sendMessage(message)

        PrizeHandle.handle(result)
        LuckyDrawManager.updateCooldown(userId, isTen = true)
    }
}


