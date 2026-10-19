@file:Suppress("unused")

package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.constant.MessageConversionEnum
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.game.fishing.FishingChainActions
import cn.chahuyun.economy.service.FishingRodService
import cn.chahuyun.economy.service.GameCoroutineService
import cn.chahuyun.economy.usecase.FishingUsecase
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 钓鱼指令入口。
 *
 * 指令保持薄入口：解析参数后交给 usecase/service 处理，具体玩法逻辑不写在 action 中。
 */
@EventComponent
class FishAction {

    @MessageAuthorize(
        text = ["钓鱼", "抛竿"],
        groupPermissions = [EconPerm.FISH_PERM]
    )
    suspend fun fishing(event: GroupMessageEvent) {
        // 阶段四试点：新规则链入口（game.fish）先跑一次，当前 @Action 只打印日志；
        // 玩法仍由旧 usecase 承载，待钓鱼重建后移入规则链并删除下面这段桥接。
        // 若 before 规则提前结束（actionExecuted=false），不再执行旧玩法。
        val execution = FishingChainActions.fish(event)
        if (execution.actionExecuted) {
            FishingUsecase.fishing(GameCoroutineService, event)
        }
    }

    @MessageAuthorize(text = ["购买鱼竿"], groupPermissions = [EconPerm.FISH_PERM])
    suspend fun buyFishRod(event: MessageEvent) {
        FishingRodService.buyFishRod(event)
    }

    @MessageAuthorize(
        text = ["升级鱼竿\\*?"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.FISH_PERM]
    )
    suspend fun upFishRod(event: MessageEvent) {
        FishingRodService.upFishRod(event)
    }

    @MessageAuthorize(text = ["鱼竿等级"], groupPermissions = [EconPerm.FISH_PERM])
    suspend fun viewFishLevel(event: MessageEvent) {
        FishingRodService.viewFishLevel(event)
    }

    @MessageAuthorize(text = ["鱼塘等级"], groupPermissions = [EconPerm.FISH_PERM])
    suspend fun viewFishPond(event: GroupMessageEvent) {
        FishingUsecase.viewFishPond(event)
    }

    @MessageAuthorize(
        text = ["创建鱼塘 ?(.+)"],
        messageMatching = MessageMatchingEnum.REGULAR,
        messageConversion = MessageConversionEnum.CONTENT,
        groupPermissions = [EconPerm.FISH_PERM]
    )
    suspend fun createPond(event: GroupMessageEvent) {
        val name = Regex("创建鱼塘 ?(.+)")
            .find(event.message.contentToString().trim())
            ?.groupValues?.get(1)?.trim().orEmpty()
        FishingUsecase.createPond(event, name)
    }

    @MessageAuthorize(
        text = ["购买鱼饵 ?(.+)"],
        messageMatching = MessageMatchingEnum.REGULAR,
        messageConversion = MessageConversionEnum.CONTENT,
        groupPermissions = [EconPerm.FISH_PERM]
    )
    suspend fun buyBait(event: GroupMessageEvent) {
        val code = Regex("购买鱼饵 ?(.+)")
            .find(event.message.contentToString().trim())
            ?.groupValues?.get(1)?.trim().orEmpty()
        FishingUsecase.buyBait(event, code)
    }

    @MessageAuthorize(text = ["钓鱼信息"], groupPermissions = [EconPerm.FISH_PERM])
    suspend fun viewFishingInfo(event: GroupMessageEvent) {
        FishingUsecase.viewFishingInfo(event)
    }

    @MessageAuthorize(text = ["钓鱼信息#"], groupPermissions = [EconPerm.FISH_PERM])
    suspend fun viewFishingInfoText(event: GroupMessageEvent) {
        FishingUsecase.viewFishingInfoText(event)
    }

    @MessageAuthorize(
        text = ["刷新钓鱼"],
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN],
        groupPermissions = [EconPerm.FISH_PERM]
    )
    suspend fun refresh(event: MessageEvent) {
        FishingUsecase.refresh(event)
    }

    @MessageAuthorize(text = ["开启 钓鱼"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun startFish(event: GroupMessageEvent) {
        FishingUsecase.startFish(event)
    }

    @MessageAuthorize(text = ["关闭 钓鱼"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun offFish(event: GroupMessageEvent) {
        FishingUsecase.offFish(event)
    }
}
