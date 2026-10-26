package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.feature.redpack.RedPackUsecaseActions
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 红包管理类，用于处理红包的创建、领取、查询等操作。
 */
@EventComponent
class RedPackAction {

    /**
     * 创建红包。
     *
     * @param event 群消息事件
     */
    @MessageAuthorize(
        text = ["发红包 \\d+(\\.\\d+)?[kKmMgGtTpPwWeE万亿]? \\d+( (sj|随机|kl|口令).*)?"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.RED_PACKET_PERM]
    )
    suspend fun create(event: GroupMessageEvent) {
        RedPackUsecaseActions.create(event)
    }

    /**
     * 领取红包（专项领取）。
     *
     * @param event 群消息事件
     */
    @MessageAuthorize(
        text = ["领红包 .+", "收红包 .+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.RED_PACKET_PERM]
    )
    suspend fun receive(event: GroupMessageEvent) {
        RedPackUsecaseActions.receive(event)
    }

    /**
     * 查询红包列表。
     *
     * @param event 群消息事件
     */
    @MessageAuthorize(
        text = ["红包列表"],
        groupPermissions = [EconPerm.RED_PACKET_PERM]
    )
    suspend fun queryRedPackList(event: GroupMessageEvent) {
        RedPackUsecaseActions.queryRedPackList(event)
    }

    /**
     * 领取全部红包（一键全抢）。
     *
     * @param event 消息事件
     */
    @MessageAuthorize(
        text = ["抢红包"],
        groupPermissions = [EconPerm.RED_PACKET_PERM]
    )
    suspend fun grabNewestRedPack(event: GroupMessageEvent) {
        RedPackUsecaseActions.grabNewestRedPack(event)
    }

    /**
     * 口令领取红包（直接发送口令）
     *
     * 走独立的 `core.redpack.grabByPassword`：无前缀口令是「被动触发」的，
     * 单独一个 actionId 才能在日志与规则里和主动指令区分开。
     */
    @MessageAuthorize(
        text = ["^[\\s\\S]{1,32}$"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.RED_PACKET_PERM]
    )
    suspend fun grabByPassword(event: GroupMessageEvent) {
        RedPackUsecaseActions.grabByPassword(event)
    }

    /**
     * 查询全局红包列表。
     *
     * @param event 消息事件
     */
    @MessageAuthorize(
        text = ["全局红包列表"],
        userPermissions = [AuthPerm.OWNER]
    )
    suspend fun queryGlobalRedPackList(event: MessageEvent) {
        RedPackUsecaseActions.queryGlobalRedPackList(event)
    }

    @MessageAuthorize(
        text = ["开启 红包"],
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun startRob(event: GroupMessageEvent) {
        RedPackUsecaseActions.startRob(event)
    }

    @MessageAuthorize(
        text = ["关闭 红包"],
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun endRob(event: GroupMessageEvent) {
        RedPackUsecaseActions.endRob(event)
    }
}
