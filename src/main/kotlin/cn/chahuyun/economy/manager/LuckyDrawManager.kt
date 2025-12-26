@file:Suppress("DuplicatedCode")

package cn.chahuyun.economy.manager

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.utils.AuthMessageUtil.sendMessageQuote
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.data.PrizesData
import cn.chahuyun.economy.entity.UserRaffle
import cn.chahuyun.economy.prizes.PrizeHandle
import cn.chahuyun.economy.prizes.PrizesUtil.draw
import cn.chahuyun.economy.prizes.PrizesUtil.drawTen
import cn.chahuyun.economy.prizes.RaffleContext
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.hibernateplus.HibernateFactory
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent

@EventComponent
class LuckyDrawManager {

    companion object {
        // 单抽冷却时间(60秒)
        private const val SINGLE_COOLDOWN = 60_000L

        // 十连冷却时间(600秒)
        private const val TEN_COOLDOWN = 600_000L

        // 用户冷却记录 (userId -> 上次抽奖时间戳)
        private val singleCooldownMap = mutableMapOf<Long, Long>()
        private val tenCooldownMap = mutableMapOf<Long, Long>()

        fun take(userId: Long): UserRaffle {
            val userRaffle = HibernateFactory.selectOneById<UserRaffle>(userId)
            if (userRaffle != null) return userRaffle
            return HibernateFactory.merge(UserRaffle(userId))
        }

        // 检查单抽冷却
        private fun checkSingleCooldown(userId: Long): Boolean {
            val lastTime = singleCooldownMap[userId] ?: 0
            return System.currentTimeMillis() - lastTime >= SINGLE_COOLDOWN
        }

        // 检查十连冷却
        private fun checkTenCooldown(userId: Long): Boolean {
            val lastTime = tenCooldownMap[userId] ?: 0
            return System.currentTimeMillis() - lastTime >= TEN_COOLDOWN
        }

        // 更新冷却时间
        private fun updateCooldown(userId: Long, isTen: Boolean = false) {
            val now = System.currentTimeMillis()
            if (isTen) {
                tenCooldownMap[userId] = now
            } else {
                singleCooldownMap[userId] = now
            }
        }
    }

    @MessageAuthorize(
        ["raffle", "抽奖"], groupPermissions = [EconPerm.RAFFLE_PERM]
    )
    suspend fun luckyDraw(event: GroupMessageEvent) {
        val sender = event.sender
        val userId = sender.id
        val userRaffle = take(userId)
        val pool = PrizesData.pool.first()
        val group = event.group
        val context = RaffleContext(pool, userRaffle, group)

        // 1. 检查单抽冷却
        if (!checkSingleCooldown(userId)) {
            val remaining =
                ((SINGLE_COOLDOWN - (System.currentTimeMillis() - (singleCooldownMap[userId] ?: 0))) / 1000).toInt()
            event.sendMessageQuote("单抽冷却中，请再等 $remaining 秒")
            return
        }

        val price = pool.price
        // 2. 修正余额检查逻辑 (原逻辑错误: >= 应为 <)
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
        // 更新单抽冷却时间
        updateCooldown(userId)
    }

    @MessageAuthorize(
        ["ten", "十连"], groupPermissions = [EconPerm.RAFFLE_PERM]
    )
    suspend fun luckyDrawTen(event: GroupMessageEvent) {
        val sender = event.sender
        val userId = sender.id
        val userRaffle = take(userId)
        val pool = PrizesData.pool.first()
        val group = event.group
        val context = RaffleContext(pool, userRaffle, group)

        // 1. 检查十连冷却
        if (!checkTenCooldown(userId)) {
            val remaining =
                ((TEN_COOLDOWN - (System.currentTimeMillis() - (tenCooldownMap[userId] ?: 0))) / 1000).toInt()
            event.sendMessageQuote("十连冷却中，请再等 $remaining 秒")
            return
        }

        val price = pool.price * 10.toDouble()
        // 2. 修正余额检查逻辑 (原逻辑错误: >= 应为 <)
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
        // 更新十连冷却时间
        updateCooldown(userId, isTen = true)
    }
}