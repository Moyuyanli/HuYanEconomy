package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.usecase.FoxBondUsecase
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 狐卷/国卷指令入口（独立模块）
 *
 * 所有指令同时支持"国卷"和"狐卷"前缀，保持兼容性。
 */
@EventComponent
class FoxBondAction {

    /**
     * 查看当前可竞标狐卷列表
     */
    @MessageAuthorize(text = ["(国卷|狐卷)(查看)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun foxView(event: MessageEvent) {
        FoxBondUsecase.foxView(event)
    }

    /**
     * 提交狐卷竞标
     */
    @MessageAuthorize(
        text = ["(国卷|狐卷)竞标 \\S+ \\d+(\\.\\d+)? \\d+(\\.\\d+)?"],
        messageMatching = MessageMatchingEnum.REGULAR
    )
    suspend fun foxBid(event: MessageEvent) {
        FoxBondUsecase.foxBid(event)
    }

    /**
     * 购买国卷（行长用流动金池资金购买本周国卷）
     */
    @MessageAuthorize(text = ["(国卷|狐卷)购买 \\d+(\\.\\d+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun buyBond(event: MessageEvent) {
        FoxBondUsecase.buyBond(event)
    }

    /**
     * 赎回国卷（不带ID赎回全部到期持仓，带ID赎回指定持仓）
     */
    @MessageAuthorize(text = ["(国卷|狐卷)赎回( \\d+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun redeemBond(event: MessageEvent) {
        FoxBondUsecase.redeemBond(event)
    }

    /**
     * 查看本周国卷发行信息 + 本行持仓列表
     */
    @MessageAuthorize(text = ["(国卷|狐卷)列表"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun bondList(event: MessageEvent) {
        FoxBondUsecase.bondList(event)
    }
}
