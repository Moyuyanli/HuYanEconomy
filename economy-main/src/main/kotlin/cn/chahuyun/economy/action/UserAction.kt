package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.economy.feature.status.UserStatusUsecaseActions
import cn.chahuyun.economy.feature.user.UserUsecaseActions
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 用户相关的“消息/事件入口”。
 *
 * 说明：
 * - 纯工具/数据逻辑统一放到 Kotlin `UserManager`（manager 包）。
 * - 这里仅保留 @MessageAuthorize 的指令处理。
 */
@EventComponent
class UserAction {

    @MessageAuthorize(text = ["个人信息", "info", "我的信息", "用户信息"])
    suspend fun getUserInfoImage(event: MessageEvent) {
        UserUsecaseActions.getUserInfoImage(event)
    }

    @MessageAuthorize(text = ["info#", "我的信息#", "用户信息#", "个人信息#", "我的#", "用户#"])
    suspend fun getUserInfoText(event: MessageEvent) {
        UserUsecaseActions.getUserInfoText(event)
    }

    @MessageAuthorize(text = ["money", "经济信息", "我的资金"])
    suspend fun moneyInfo(event: MessageEvent) {
        UserUsecaseActions.moneyInfo(event)
    }

    @MessageAuthorize(text = ["出院"])
    suspend fun discharge(event: GroupMessageEvent) {
        // 出院改的是位置状态、扣的是医药费，业务归属 feature/status。
        UserStatusUsecaseActions.discharge(event)
    }
}


