package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.usecase.LotteryUsecase
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 彩票管理
 * 3种彩票：
 * 一分钟开一次
 * 一小时开一次
 * 一天开一次
 */
@EventComponent
class LotteryAction {

    @MessageAuthorize(
        text = ["猜签 (\\d+)( \\d+)", "lottery (\\d+)( \\d+)"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.LOTTERY_PERM]
    )
    suspend fun addLottery(event: GroupMessageEvent) {
        LotteryUsecase.addLottery(event)
    }

    @MessageAuthorize(text = ["开启 猜签"], userPermissions = [cn.chahuyun.authorize.constant.AuthPerm.OWNER, cn.chahuyun.authorize.constant.AuthPerm.ADMIN])
    suspend fun startLottery(event: GroupMessageEvent) {
        LotteryUsecase.startLottery(event)
    }

    @MessageAuthorize(text = ["关闭 猜签"], userPermissions = [cn.chahuyun.authorize.constant.AuthPerm.OWNER, cn.chahuyun.authorize.constant.AuthPerm.ADMIN])
    suspend fun endLottery(event: GroupMessageEvent) {
        LotteryUsecase.endLottery(event)
    }
}
