package cn.chahuyun.economy.prizes

import cn.chahuyun.economy.constant.PrizeType
import cn.chahuyun.economy.exception.RaffleException
import cn.chahuyun.economy.utils.EconomyUtil
import net.mamoe.mirai.Bot

@Suppress("unused")
object PrizeHandle {

    fun handle(prizes: List<RaffleResult>) {
        prizes.forEach {
            when (it.prize.type) {
                PrizeType.ORDINARY -> return@forEach
                PrizeType.MONEY -> handleMoney(it)
                PrizeType.TITLE -> handleTitle(it)
                PrizeType.PROP -> handleProp(it)
                PrizeType.PUNISHMENT -> handlePunishment(it)
            }
        }
    }


    private fun handleMoney(raffleResult: RaffleResult) {
        val prize = raffleResult.prize
        val userId = raffleResult.userId
        val groupId = raffleResult.groupId
        val member = Bot.instances.first().getGroup(groupId)?.get(userId) ?: throw RaffleException("兑奖异常")
        val money = prize.metadata["money"] ?: throw RaffleException("兑奖异常,奖金为空!")
        EconomyUtil.plusMoneyToUser(member, money.toDouble())
    }

    private fun handleTitle(raffleResult: RaffleResult) {
        TODO()
    }

    private fun handleProp(raffleResult: RaffleResult) {
        TODO()
    }

    private fun handlePunishment(raffleResult: RaffleResult) {
        TODO()
    }
}