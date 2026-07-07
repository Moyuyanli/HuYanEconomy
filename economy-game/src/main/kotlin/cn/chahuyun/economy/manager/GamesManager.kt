package cn.chahuyun.economy.manager

import cn.chahuyun.economy.model.fish.FishInfoDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.service.*
import cn.chahuyun.economy.utils.Log
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User

/**
 * 游戏模块兼容门面。
 *
 * 新代码优先使用 `service` 包中的细分服务；这里保留旧入口，避免 main/action 和历史调用点一次性大改。
 */
object GamesManager {

    @JvmStatic
    fun init() {
        GameCoroutineService.init()
        FishPondUpgradeService.init()
    }

    @JvmStatic
    fun shutdown() {
        Log.info("游戏管理:正在关闭游戏线程...")
        FishPondUpgradeService.shutdown()
        GameCoroutineService.shutdown()
    }

    @JvmStatic
    fun clearCooling() {
        FishingRuntimeService.clearCooling()
    }

    @JvmStatic
    fun removeCooling(qq: Long) {
        FishingRuntimeService.removeCooling(qq)
    }

    @JvmStatic
    fun getFishingCooldownText(userInfo: UserInfoDto, isFishingTitle: Boolean, fishInfo: FishInfoDto): String =
        FishingRuntimeService.getFishingCooldownText(userInfo, isFishingTitle, fishInfo)

    @JvmStatic
    suspend fun checkAndProcessFishing(
        userInfo: UserInfoDto,
        isFishingTitle: Boolean,
        fishInfo: FishInfoDto,
        subject: Contact,
        chain: net.mamoe.mirai.message.data.MessageChain,
    ): Boolean =
        FishingRuntimeService.checkAndProcessFishing(userInfo, isFishingTitle, fishInfo, subject, chain)

    @JvmStatic
    suspend fun failedFishing(userInfo: UserInfoDto, user: User, subject: Contact, fishInfo: FishInfoDto): Boolean =
        FishingRuntimeService.failedFishing(userInfo, user, subject, fishInfo)

    /**
     * 下列函数目前未通过 @MessageAuthorize 直接暴露，保留为可复用的业务接口。
     */
    @JvmStatic
    suspend fun buyFishRod(event: net.mamoe.mirai.event.events.MessageEvent) {
        FishingRodService.buyFishRod(event)
    }

    @JvmStatic
    suspend fun upFishRod(event: net.mamoe.mirai.event.events.MessageEvent) {
        FishingRodService.upFishRod(event)
    }

    @JvmStatic
    suspend fun fishTop(event: net.mamoe.mirai.event.events.MessageEvent) {
        FishingRankingService.fishTop(event)
    }

    @JvmStatic
    suspend fun viewFishLevel(event: net.mamoe.mirai.event.events.MessageEvent) {
        FishingRodService.viewFishLevel(event)
    }
}
