package cn.chahuyun.economy.service

import cn.chahuyun.economy.model.LotteryInfoDto
import cn.chahuyun.economy.runtime.EconomyRuntime
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.NormalMember

object LotteryPayoutService {

    fun result(type: Int, location: Int, lotteryInfo: LotteryInfoDto) {
        if (location == 0) {
            LotteryDataService.delete(lotteryInfo)
            return
        }
        val bot: Bot = EconomyRuntime.bot ?: return
        val group = bot.getGroup(lotteryInfo.group) ?: return
        val member: NormalMember = group.get(lotteryInfo.qq) ?: return
        LotteryDataService.delete(lotteryInfo)

        if (!EconomyAccountService.addWallet(member, lotteryInfo.bonus)) {
            runBlocking { member.sendMessage("奖金添加失败，请联系管理员") }
            return
        }

        runBlocking { member.sendMessage(LotteryMessageFormatter.result(lotteryInfo)) }
        when (type) {
            1 -> if (location == 3) {
                runBlocking {
                    group.sendMessage(
                        LotteryMessageFormatter.groupWinner(member, lotteryInfo.bonus)
                    )
                }
            }

            2 -> if (location == 4) {
                runBlocking {
                    group.sendMessage(
                        LotteryMessageFormatter.groupWinner(member, lotteryInfo.bonus)
                    )
                }
            }

            3 -> if (location == 5) {
                runBlocking {
                    group.sendMessage(
                        LotteryMessageFormatter.groupWinner(member, lotteryInfo.bonus)
                    )
                }
            }
        }
    }
}
