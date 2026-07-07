package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.constant.MessageConversionEnum
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.usecase.UserStatusUsecase
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 用户状态管理
 */
@EventComponent
class UserStatusAction {

    @MessageAuthorize(text = ["我的状态", "我的位置"])
    suspend fun myStatus(event: GroupMessageEvent) {
        UserStatusUsecase.myStatus(event)
    }

    @MessageAuthorize(text = ["回家"])
    suspend fun goHome(event: GroupMessageEvent) {
        UserStatusUsecase.goHome(event)
    }

    @MessageAuthorize(
        text = ["回家 ?@\\d+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        messageConversion = MessageConversionEnum.CONTENT,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun gotoHome(event: GroupMessageEvent) {
        UserStatusUsecase.gotoHome(event)
    }
}
