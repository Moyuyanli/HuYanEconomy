package cn.chahuyun.economy.service

import cn.chahuyun.economy.data.repository.UserFishingInfoRepository
import cn.chahuyun.economy.entity.fishing.UserFishingInfoEntity
import cn.chahuyun.economy.games.GamesReplySupport
import cn.chahuyun.economy.runtime.EconomyRuntime
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.event.events.MessageEvent
import java.math.BigDecimal

/**
 * 鱼竿服务：购买、升级、查询。
 *
 * 鱼竿等级保存在 `hye_user_fishing_info.rod_level`，0 表示尚未购买；
 * 所有消耗通过 [FishingAccountService] 走 economy-base 用户账户。
 */
object FishingRodService {

    /** 单次升级上限，避免自动升级时无限循环。 */
    private const val AUTO_UPGRADE_LIMIT = 200

    suspend fun buyFishRod(event: MessageEvent) {
        Log.info("购买鱼竿指令")
        val userInfo = EconomyUserService.getOrCreate(event.sender)
        val fishInfo = UserFishingInfoRepository.getOrCreate(FishingIdentity.userId(userInfo))
        val msg = EconomyRuntime.msgConfig

        if (fishInfo.rodLevel > 0) {
            GamesReplySupport.reply(event, msg.repeatPurchaseRod)
            return
        }

        val price = BigDecimal.valueOf(msg.buyRodPrice.toLong())
        if (FishingAccountService.userBalance(userInfo.qq) < price) {
            GamesReplySupport.reply(event, msg.coinNotEnoughForRod)
            return
        }

        if (FishingAccountService.deductUser(userInfo.qq, price)) {
            fishInfo.rodLevel = 1
            fishInfo.touch()
            UserFishingInfoRepository.save(fishInfo)
            GamesReplySupport.reply(event, msg.buyFishingRodSuccess)
        } else {
            Log.error("钓鱼:购买鱼竿扣款失败 qq=${userInfo.qq}")
        }
    }

    suspend fun upFishRod(event: MessageEvent) {
        Log.info("升级鱼竿指令")
        val userInfo = EconomyUserService.getOrCreate(event.sender)
        val fishInfo = UserFishingInfoRepository.getOrCreate(FishingIdentity.userId(userInfo))
        val msg = EconomyRuntime.msgConfig

        if (fishInfo.rodLevel <= 0) {
            GamesReplySupport.reply(event, msg.noneRodUpgradeMsg)
            return
        }
        if (FishingRuntimeService.isFishing(userInfo.qq)) {
            GamesReplySupport.reply(event, msg.upgradeWhenFishing)
            return
        }

        val auto = event.message.contentToString().trim().endsWith("*")
        val result = if (auto) autoUpgrade(userInfo.qq, fishInfo) else upgradeOnce(userInfo.qq, fishInfo)
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, result))
    }

    suspend fun viewFishLevel(event: MessageEvent) {
        Log.info("鱼竿等级指令")
        val userInfo = EconomyUserService.getOrCreate(event.sender)
        val fishInfo = UserFishingInfoRepository.getOrCreate(FishingIdentity.userId(userInfo))
        val text = if (fishInfo.rodLevel <= 0) {
            EconomyRuntime.msgConfig.noneRodUpgradeMsg
        } else {
            "你的鱼竿等级:${fishInfo.rodLevel}"
        }
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, text))
    }

    private fun upgradeOnce(qq: Long, fishInfo: UserFishingInfoEntity): String {
        val msg = EconomyRuntime.msgConfig
        val cost = FishingRodUpgradeRules.upgradeCost(
            fishInfo.rodLevel,
            FishingRodUpgradeRules.fishingLevel(fishInfo.rodLevel),
        ) ?: return msg.rodMaxLevel

        val price = BigDecimal.valueOf(cost.toLong())
        if (FishingAccountService.userBalance(qq) < price) return msg.coinNotEnoughForRodUpgrade
        if (!FishingAccountService.deductUser(qq, price)) return "鱼竿升级失败!"

        fishInfo.rodLevel += 1
        fishInfo.touch()
        UserFishingInfoRepository.save(fishInfo)
        return msg.upgradeRodSuccess.format(fishInfo.rodLevel)
    }

    private fun autoUpgrade(qq: Long, fishInfo: UserFishingInfoEntity): String {
        val msg = EconomyRuntime.msgConfig
        var upgraded = 0
        var lastMessage = msg.rodMaxLevel

        while (upgraded < AUTO_UPGRADE_LIMIT) {
            val cost = FishingRodUpgradeRules.upgradeCost(
                fishInfo.rodLevel,
                FishingRodUpgradeRules.fishingLevel(fishInfo.rodLevel),
            ) ?: break
            val price = BigDecimal.valueOf(cost.toLong())
            if (FishingAccountService.userBalance(qq) < price) {
                return if (upgraded == 0) msg.coinNotEnoughForRodUpgrade else "连续升级 $upgraded 级\n$lastMessage"
            }
            if (!FishingAccountService.deductUser(qq, price)) break
            fishInfo.rodLevel += 1
            upgraded += 1
            lastMessage = msg.upgradeRodSuccess.format(fishInfo.rodLevel)
        }

        if (upgraded == 0) return msg.rodMaxLevel
        fishInfo.touch()
        UserFishingInfoRepository.save(fishInfo)
        return "连续升级 $upgraded 级\n$lastMessage"
    }
}
