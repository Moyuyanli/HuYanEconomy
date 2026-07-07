package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.usecase.TransferUsecase
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 转账管理
 * 转账 | 作弊
 */
@EventComponent
class TransferAction {

    /**
     * 用户转账给另一个用户操作
     */
    @MessageAuthorize(
        text = ["转账(\\[mirai:at:\\d+])? \\d+( \\d+)?"],
        messageMatching = MessageMatchingEnum.REGULAR
    )
    suspend fun userToUser(event: MessageEvent) {
        TransferUsecase.userToUser(event)
    }

    @MessageAuthorize(
        text = ["greedisgood \\d+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun cheat(event: MessageEvent) {
        TransferUsecase.cheat(event)
    }
}
