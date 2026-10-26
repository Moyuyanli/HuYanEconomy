package cn.chahuyun.economy.games.lottery

import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.actionchain.annotation.Action
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.games.GamesReplySupport
import cn.chahuyun.economy.model.LotteryInfoDto
import cn.chahuyun.economy.service.EconomyAccountService
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MoneyFormatUtil
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 猜签玩法的主用例。
 *
 * 三种签按号码位数区分：三位小签（每分钟开）、四位中签（每小时开）、五位大签（每天开）。
 *
 * ## 动作清单
 * | actionId | 指令 | 规则链 |
 * |---|---|---|
 * | `game.lottery.bet` | 猜签 号码 金额 | `@Before(10)` 解析 → `@Before(20)` 余额 → `@Before(30)` 签种与上限 |
 * | `game.lottery.open` | 开启 猜签 | `@Before(10)` 是否已开启 |
 * | `game.lottery.close` | 关闭 猜签 | `@Before(10)` 是否已开启 |
 *
 * 投注的所有校验都在 [LotteryRules]，本文件只负责扣款、落库、回复与定时任务排期。
 */
object LotteryUsecase {

    /**
     * 投注一张签（`game.lottery.bet`）。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文，读取 [LotteryBet]。
     */
    @Action("game.lottery.bet")
    suspend fun bet(event: GroupMessageEvent, context: ActionContext) {
        Log.info("彩票指令:用户(${event.sender.id})")

        val actor = context.provide(LotteryActor::class, LotteryProvider.ACTOR)
        val bet = context.get(LotteryBet::class) ?: return

        if (!EconomyAccountService.subtractWallet(actor.user, bet.money)) {
            GamesReplySupport.reply(actor.subject, actor.message, "猜签失败！")
            return
        }

        LotteryService.save(
            LotteryInfoDto(
                qq = actor.user.id,
                group = actor.subject.id,
                money = bet.money,
                type = bet.type,
                number = bet.number,
            ),
        )

        GamesReplySupport.reply(
            actor.subject,
            actor.message,
            "猜签成功:\n" +
                "猜签类型:${bet.typeName}\n" +
                "猜签号码:${bet.number}\n" +
                "猜签金币:${MoneyFormatUtil.format(bet.money)}",
        )

        LotteryService.ensureSchedule(bet.type)
    }

    /**
     * 开启本群猜签（`game.lottery.open`）。
     *
     * @param event 群消息事件。
     */
    @Action("game.lottery.open")
    suspend fun startLottery(event: GroupMessageEvent) {
        Log.info("开启猜签:群(${event.group.id})")
        val group = UserUtil.group(event.group.id)
        val message = if (PermUtil.addUserToPermGroupByName(group, EconPerm.GROUP.LOTTERY_PERM_GROUP)) {
            "本群的猜签开启成功!"
        } else {
            "本群的猜签开启失败!"
        }
        GamesReplySupport.reply(event, message)
    }

    /**
     * 关闭本群猜签（`game.lottery.close`）。
     *
     * @param event 群消息事件。
     */
    @Action("game.lottery.close")
    suspend fun endLottery(event: GroupMessageEvent) {
        Log.info("关闭猜签:群(${event.group.id})")
        val group = UserUtil.group(event.group.id)
        val permGroup: PermGroup = PermUtil.takePermGroupByName(EconPerm.GROUP.LOTTERY_PERM_GROUP)
        permGroup.users.remove(group)
        permGroup.save()
        GamesReplySupport.reply(event, "本群的猜签关闭成功!")
    }
}
