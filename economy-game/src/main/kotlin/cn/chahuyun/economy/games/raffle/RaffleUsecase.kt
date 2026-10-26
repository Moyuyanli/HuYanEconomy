package cn.chahuyun.economy.games.raffle

import cn.chahuyun.economy.actionchain.annotation.Action
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.games.GamesReplySupport
import cn.chahuyun.economy.service.EconomyAccountService
import cn.chahuyun.economy.service.EconomyRaffleService
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.Message

/**
 * 抽奖玩法的主用例。
 *
 * ## 动作清单
 * | actionId | 指令 | 规则链 |
 * |---|---|---|
 * | `game.raffle.draw` | 抽奖 / raffle | `@Before(10)` 单抽冷却 → `@Before(20)` 余额 |
 * | `game.raffle.ten` | 十连 / ten | `@Before(10)` 十连冷却 → `@Before(20)` 余额 |
 *
 * 冷却与余额判定都在 [RaffleRules]；本文件只做扣款、抽奖、发奖与记冷却。
 * 十连结果用合并转发发出，单抽结果直接引用回复。
 */
object RaffleUsecase {

    /**
     * 单抽（`game.raffle.draw`）。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文。
     */
    @Action("game.raffle.draw")
    suspend fun luckyDraw(event: GroupMessageEvent, context: ActionContext) {
        Log.info("抽奖指令:单抽 用户(${event.sender.id})")

        val actor = context.provide(RaffleActor::class, RaffleProvider.ACTOR)
        val price = EconomyRaffleService.defaultPoolPrice()
        val userRaffle = RaffleService.takeUserRaffle(actor.user.id)

        if (!EconomyAccountService.subtractWallet(actor.user, price)) {
            GamesReplySupport.quote(event, "抽奖失败!")
            return
        }

        val result = EconomyRaffleService.drawSingle(userRaffle, actor.subject)
        val prize = result.firstPrize

        GamesReplySupport.reply(
            actor.subject,
            actor.user.id,
            singleDrawMessage(actor.user.nameCardOrNick, prize.name, prize.description),
        )

        EconomyRaffleService.handle(result)
        RaffleService.updateCooldown(actor.user.id)
    }

    /**
     * 十连（`game.raffle.ten`）。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文。
     */
    @Action("game.raffle.ten")
    suspend fun luckyDrawTen(event: GroupMessageEvent, context: ActionContext) {
        Log.info("抽奖指令:十连 用户(${event.sender.id})")

        val actor = context.provide(RaffleActor::class, RaffleProvider.ACTOR)
        val price = EconomyRaffleService.defaultPoolPrice(10)
        val userRaffle = RaffleService.takeUserRaffle(actor.user.id)

        if (!EconomyAccountService.subtractWallet(actor.user, price)) {
            GamesReplySupport.quote(event, "抽奖失败!")
            return
        }

        val result = EconomyRaffleService.drawTen(userRaffle, actor.subject)
        GamesReplySupport.send(event, tenDrawMessage(event, result.prizes.map { it.name to it.description }))

        EconomyRaffleService.handle(result)
        RaffleService.updateCooldown(actor.user.id, isTen = true)
    }

    /** 单抽结果文案。 */
    private fun singleDrawMessage(nick: String, prizeName: String, description: String): String =
        """
        $nick 本次抽奖结果: $prizeName
        $description
        """.trimIndent()

    /**
     * 十连结果：合并转发，一条奖品一层，标题与预览沿用原有玩梗文案。
     *
     * @param event 群消息事件（需要 bot 与被 @ 的随机群友来拼预览）。
     * @param prizes 奖品名与描述。
     */
    private fun tenDrawMessage(event: GroupMessageEvent, prizes: List<Pair<String, String>>): Message {
        val sender = event.sender
        val random = event.group.members.random()
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
