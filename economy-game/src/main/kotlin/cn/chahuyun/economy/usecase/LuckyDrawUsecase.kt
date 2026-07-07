package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.service.EconomyAccountService
import cn.chahuyun.economy.service.EconomyRaffleService
import cn.chahuyun.economy.service.LuckyDrawService
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.Message

object LuckyDrawUsecase {

    suspend fun luckyDraw(event: GroupMessageEvent) {
        val sender = event.sender
        val userId = sender.id
        val userRaffle = LuckyDrawService.takeUserRaffle(userId)
        val group = event.group

        // 1. 检查单抽冷却
        if (!LuckyDrawService.checkSingleCooldown(userId)) {
            val remaining = LuckyDrawService.singleRemainingSeconds(userId)
            GameUsecaseReplySupport.quote(event, "单抽冷却中，请再等 $remaining 秒")
            return
        }

        val price = EconomyRaffleService.defaultPoolPrice()
        if (EconomyAccountService.walletBalance(sender) < price.toDouble()) {
            GameUsecaseReplySupport.quote(event, "你的余额不足 $price ,无法抽奖")
            return
        }
        if (!EconomyAccountService.subtractWallet(sender, price.toDouble())) {
            GameUsecaseReplySupport.quote(event, "抽奖失败!")
            return
        }

        val result = EconomyRaffleService.drawSingle(userRaffle, group)
        val prize = result.firstPrize

        GameUsecaseReplySupport.reply(
            group,
            userId,
            singleDrawMessage(sender.nameCardOrNick, prize.name, prize.description)
        )

        EconomyRaffleService.handle(result)
        LuckyDrawService.updateCooldown(userId)
    }

    suspend fun luckyDrawTen(event: GroupMessageEvent) {
        val sender = event.sender
        val userId = sender.id
        val userRaffle = LuckyDrawService.takeUserRaffle(userId)
        val group = event.group

        // 1. 检查十连冷却
        if (!LuckyDrawService.checkTenCooldown(userId)) {
            val remaining = LuckyDrawService.tenRemainingSeconds(userId)
            GameUsecaseReplySupport.quote(event, "十连冷却中，请再等 $remaining 秒")
            return
        }

        val price = EconomyRaffleService.defaultPoolPrice(10)
        if (EconomyAccountService.walletBalance(sender) < price) {
            GameUsecaseReplySupport.quote(event, "你的余额不足 $price ,无法抽奖")
            return
        }
        if (!EconomyAccountService.subtractWallet(sender, price)) {
            GameUsecaseReplySupport.quote(event, "抽奖失败!")
            return
        }

        val result = EconomyRaffleService.drawTen(userRaffle, group)
        val message = tenDrawMessage(event, result.prizes.map { it.name to it.description })

        GameUsecaseReplySupport.send(event, message)

        EconomyRaffleService.handle(result)
        LuckyDrawService.updateCooldown(userId, isTen = true)
    }

    private fun singleDrawMessage(nick: String, prizeName: String, description: String): String =
        """
        $nick 本次抽奖结果: $prizeName
        $description
        """.trimIndent()

    private fun tenDrawMessage(event: GroupMessageEvent, prizes: List<Pair<String, String>>): Message {
        val sender = event.sender
        val group = event.group
        val random = group.members.random()
        val nick = sender.nameCardOrNick
        return MessageUtil.INSTANCE.buildForwardMessage(
            event,
            titleGenerator = "群聊的聊天记录",
            previewGenerator = listOf(
                "$nick:卧槽,中了!中了!!",
                "${event.bot.nameCardOrNick}:真的假滴?彩票滞销了?",
                "${random.nameCardOrNick}:我不信,除非你给我!"
            ),
            summarySize = 11
        ) {
            event.bot named event.bot.nameCardOrNick says "以下是你本次的10连结果↓:"
            prizes.forEach { (name, description) ->
                sender.id named nick says """
                    $name
                    $description
                """.trimIndent()
            }
        }
    }
}
