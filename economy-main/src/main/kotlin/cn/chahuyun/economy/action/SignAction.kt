package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.usecase.SignUsecase
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 签到管理
 */
@EventComponent
class SignAction {

    /**
     * 签到
     */
    @MessageAuthorize(text = ["签到", "打卡", "sign"], blackPermissions = [EconPerm.SIGN_BLACK_PERM])
    suspend fun sign(event: GroupMessageEvent) {
        SignUsecase.sign(event)
    }

    @MessageAuthorize(text = ["关闭 签到"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun offSign(event: GroupMessageEvent) {
        SignUsecase.offSign(event)
    }

    @MessageAuthorize(text = ["开启 签到"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun startSign(event: GroupMessageEvent) {
        SignUsecase.startSign(event)
    }

    @MessageAuthorize(text = ["刷新签到"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun refreshSign(event: GroupMessageEvent) {
        SignUsecase.refreshSign(event)
    }
}
