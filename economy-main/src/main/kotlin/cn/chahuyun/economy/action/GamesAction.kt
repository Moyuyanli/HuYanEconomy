@file:Suppress("unused")

package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.manager.GamesManager
import cn.chahuyun.economy.service.GameCoroutineService
import cn.chahuyun.economy.usecase.GamesUsecase
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 游戏管理
 * 钓鱼重构为 Kotlin
 *
 * @author Moyuyanli
 * @date 2025/12/22
 */
@EventComponent
class GamesAction {

    @MessageAuthorize(
        text = ["钓鱼", "抛竿"],
        groupPermissions = [EconPerm.FISH_PERM]
    )
    suspend fun fishing(event: GroupMessageEvent) {
        GamesUsecase.fishing(GameCoroutineService, event)
    }

    @MessageAuthorize(text = ["购买鱼竿"], groupPermissions = [EconPerm.FISH_PERM])
    suspend fun buyFishRod(event: MessageEvent) {
        GamesManager.buyFishRod(event)
    }

    @MessageAuthorize(
        text = ["刷新钓鱼"],
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN],
        groupPermissions = [EconPerm.FISH_PERM]
    )
    suspend fun refresh(event: MessageEvent) {
        GamesUsecase.refresh(event)
    }

    @MessageAuthorize(text = ["开启 钓鱼"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun startFish(event: GroupMessageEvent) {
        GamesUsecase.startFish(event)
    }

    @MessageAuthorize(text = ["关闭 钓鱼"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun offFish(event: GroupMessageEvent) {
        GamesUsecase.offFish(event)
    }

    @MessageAuthorize(text = ["鱼塘等级"], groupPermissions = [EconPerm.FISH_PERM])
    suspend fun viewFishPond(event: GroupMessageEvent) {
        GamesUsecase.viewFishPond(event)
    }

    @MessageAuthorize(text = ["鱼竿等级"], groupPermissions = [EconPerm.FISH_PERM])
    suspend fun viewFishLevel(event: MessageEvent) {
        GamesManager.viewFishLevel(event)
    }

    @MessageAuthorize(text = ["钓鱼信息"], groupPermissions = [EconPerm.FISH_PERM])
    suspend fun viewFishingInfo(event: GroupMessageEvent) {
        GamesUsecase.viewFishingInfo(event)
    }

    @MessageAuthorize(text = ["钓鱼信息#"], groupPermissions = [EconPerm.FISH_PERM])
    suspend fun viewFishingInfoText(event: GroupMessageEvent) {
        GamesUsecase.viewFishingInfoText(event)
    }

    @MessageAuthorize(
        text = ["升级鱼竿\\*?"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.FISH_PERM]
    )
    suspend fun upFishRod(event: MessageEvent) {
        GamesManager.upFishRod(event)
    }

}
