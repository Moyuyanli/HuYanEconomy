package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.economy.usecase.UserUsecase
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 用户相关的“消息/事件入口”。
 *
 * 说明：
 * - 纯工具/数据逻辑统一放到 Kotlin `UserCoreManager`（manager 包）。
 * - 这里仅保留 @MessageAuthorize 的指令处理。
 */
@EventComponent
class UserAction {

    @MessageAuthorize(text = ["个人信息", "info"])
    suspend fun getUserInfoImage(event: MessageEvent) {
        UserUsecase.getUserInfoImage(event)
    }

    @MessageAuthorize(text = ["money", "经济信息", "我的资金"])
    suspend fun moneyInfo(event: MessageEvent) {
        UserUsecase.moneyInfo(event)
    }

    @MessageAuthorize(text = ["出院"])
    suspend fun discharge(event: GroupMessageEvent) {
        UserUsecase.discharge(event)
    }
}


