@file:Suppress("unused")

package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.fish.FishRollEvent
import cn.chahuyun.economy.fish.FishStartEvent
import cn.chahuyun.economy.manager.GamesManager
import cn.chahuyun.economy.usecase.GamesUsecase
import kotlinx.coroutines.CoroutineScope
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import kotlin.coroutines.CoroutineContext

/**
 * 游戏管理
 * 钓鱼重构为 Kotlin
 *
 * @author Moyuyanli
 * @date 2025/12/22
 */
@EventComponent
class GamesAction : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = GamesManager.coroutineContext

    companion object {

        @JvmStatic
        suspend fun fishStart(event: FishStartEvent) {
            GamesUsecase.fishStart(event)
        }

        @JvmStatic
        fun fishRoll(event: FishRollEvent) {
            GamesUsecase.fishRoll(event)
        }
    }

    @MessageAuthorize(
        text = ["钓鱼", "抛竿"],
        groupPermissions = [EconPerm.FISH_PERM]
    )
    suspend fun fishing(event: GroupMessageEvent) {
        GamesUsecase.fishing(this, event)
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

}
