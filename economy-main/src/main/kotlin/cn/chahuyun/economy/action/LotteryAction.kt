package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.games.lottery.LotteryUsecaseActions
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 猜签指令入口。
 *
 * 三种签按号码位数区分：
 * - 三位 → 小签，每分钟开一次
 * - 四位 → 中签，每小时开一次
 * - 五位 → 大签，每天开一次
 */
@EventComponent
class LotteryAction {

    @MessageAuthorize(
        text = ["猜签 \\d+ \\d+(\\.\\d+)?[kKmMgGtTpPwWeE万亿]?", "lottery \\d+ \\d+(\\.\\d+)?[kKmMgGtTpPwWeE万亿]?"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.LOTTERY_PERM]
    )
    suspend fun addLottery(event: GroupMessageEvent) {
        LotteryUsecaseActions.bet(event)
    }

    @MessageAuthorize(text = ["开启 猜签"], userPermissions = [cn.chahuyun.authorize.constant.AuthPerm.OWNER, cn.chahuyun.authorize.constant.AuthPerm.ADMIN])
    suspend fun startLottery(event: GroupMessageEvent) {
        LotteryUsecaseActions.startLottery(event)
    }

    @MessageAuthorize(text = ["关闭 猜签"], userPermissions = [cn.chahuyun.authorize.constant.AuthPerm.OWNER, cn.chahuyun.authorize.constant.AuthPerm.ADMIN])
    suspend fun endLottery(event: GroupMessageEvent) {
        LotteryUsecaseActions.endLottery(event)
    }
}
