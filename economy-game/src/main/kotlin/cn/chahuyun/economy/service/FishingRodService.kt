package cn.chahuyun.economy.service

import cn.chahuyun.economy.model.fish.save
import cn.chahuyun.economy.model.fish.updateRod
import cn.chahuyun.economy.model.user.getFishInfo
import cn.chahuyun.economy.runtime.EconomyRuntime
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.event.events.MessageEvent

object FishingRodService {

    suspend fun buyFishRod(event: MessageEvent) {
        Log.info("购买鱼竿指令")
        val userInfo = EconomyUserService.getOrCreate(event.sender)
        val fishInfo = userInfo.getFishInfo()
        val subject = event.subject

        if (fishInfo.isFishRod) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    EconomyRuntime.msgConfig.repeatPurchaseRod
                )
            )
            return
        }

        val moneyByUser = EconomyAccountService.walletBalance(event.sender)
        if (moneyByUser < 500) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    EconomyRuntime.msgConfig.coinNotEnoughForRod
                )
            )
            return
        }

        if (EconomyAccountService.subtractWallet(event.sender, 500.0)) {
            fishInfo.isFishRod = true
            fishInfo.save()
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    EconomyRuntime.msgConfig.buyFishingRodSuccess
                )
            )
        } else {
            Log.error("游戏管理:购买鱼竿失败!")
        }
    }

    suspend fun upFishRod(event: MessageEvent) {
        Log.info("升级鱼竿指令")
        val userInfo = EconomyUserService.getOrCreate(event.sender)
        val subject = event.subject
        val fishInfo = userInfo.getFishInfo()

        if (!fishInfo.isFishRod) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    EconomyRuntime.msgConfig.noneRodUpgradeMsg
                )
            )
            return
        }
        if (fishInfo.status) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    EconomyRuntime.msgConfig.upgradeWhenFishing
                )
            )
            return
        }
        subject.sendMessage(fishInfo.updateRod(userInfo))
    }

    suspend fun viewFishLevel(event: MessageEvent) {
        Log.info("鱼竿等级指令")
        val userInfo = EconomyUserService.getOrCreate(event.sender)
        val rodLevel = userInfo.getFishInfo().rodLevel
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, FishingRodMessageFormatter.currentLevel(rodLevel)))
    }
}
