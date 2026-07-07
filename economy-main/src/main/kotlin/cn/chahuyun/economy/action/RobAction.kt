package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.constant.MessageConversionEnum
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.usecase.RobUsecase
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 抢劫管理
 */
@EventComponent
class RobAction {

    @MessageAuthorize(
        text = ["抢劫 ?@?\\d{6,11} ?"],
        messageMatching = MessageMatchingEnum.REGULAR,
        messageConversion = MessageConversionEnum.CONTENT,
        groupPermissions = [EconPerm.ROB_PERM]
    )
    suspend fun rob(event: GroupMessageEvent) {
        RobUsecase.rob(event)
    }

    @MessageAuthorize(
        text = ["打人 ?@?\\d{6,11} ?"],
        messageMatching = MessageMatchingEnum.REGULAR,
        messageConversion = MessageConversionEnum.CONTENT,
        groupPermissions = [EconPerm.ROB_PERM]
    )
    suspend fun hit(event: GroupMessageEvent) {
        RobUsecase.hit(event)
    }

    @MessageAuthorize(text = ["开启 抢劫"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun startRob(event: GroupMessageEvent) {
        RobUsecase.startRob(event)
    }

    @MessageAuthorize(text = ["关闭 抢劫"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun endRob(event: GroupMessageEvent) {
        RobUsecase.endRob(event)
    }
}
