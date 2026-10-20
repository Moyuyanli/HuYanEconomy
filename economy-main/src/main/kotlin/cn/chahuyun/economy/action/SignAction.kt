package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.feature.sign.SignUsecaseActions
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 签到指令入口。
 *
 * 只做三件事：指令匹配（`@MessageAuthorize`）、权限/黑名单声明、转发到对应动作。
 * 业务逻辑一行都不写——真正的流程在 `feature/sign/SignUsecase`，前后扩展点在
 * `feature/sign/SignRules`，通过 KSP 生成的门面 [SignUsecaseActions] 进入规则链。
 */
@EventComponent
class SignAction {

    /**
     * 签到（`core.sign`）。
     *
     * 规则链：`@Before` 资格校验 → 奖励计算；动作本体结算；`@After` 日志 → 称号检查。
     * `blackPermissions` 让被关掉签到的群无法触发本指令（关闭/开启签到就是改这个黑名单组）。
     */
    @MessageAuthorize(text = ["签到", "打卡", "sign"], blackPermissions = [EconPerm.SIGN_BLACK_PERM])
    suspend fun sign(event: GroupMessageEvent) {
        SignUsecaseActions.sign(event)
    }

    /**
     * 关闭本群签到（`core.sign.close`）。
     *
     * 仅群主/管理员可发；执行结果由动作本体直接回复，后置规则只做审计日志。
     */
    @MessageAuthorize(text = ["关闭 签到"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun closeSign(event: GroupMessageEvent) {
        SignUsecaseActions.closeSign(event)
    }

    /**
     * 开启本群签到（`core.sign.open`）。
     */
    @MessageAuthorize(text = ["开启 签到"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun openSign(event: GroupMessageEvent) {
        SignUsecaseActions.openSign(event)
    }

    /**
     * 刷新签到状态（`core.sign.refresh`）。
     *
     * 管理员纠错用：把 `signTime` 往前拨一天，使目标群成员可以立刻再签一次。
     */
    @MessageAuthorize(text = ["刷新签到"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun refreshSign(event: GroupMessageEvent) {
        SignUsecaseActions.refreshSign(event)
    }
}
