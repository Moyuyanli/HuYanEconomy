package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.usecase.FoxBondUsecase
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 国卷指令入口（独立模块）
 *
 * 狐卷属于私银模块，入口保留在 PrivateBankAction，避免同一条狐卷消息被两个监听器重复响应。
 */
@EventComponent
class FoxBondAction {

    /**
     * 查看本周国卷发行信息 + 本行持仓列表
     */
    @MessageAuthorize(text = ["国卷( 查看|查看)?", "国卷列表"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun bondList(event: MessageEvent) {
        FoxBondUsecase.bondList(event)
    }

    /**
     * 购买国卷（行长用流动金池资金购买本周国卷）
     */
    @MessageAuthorize(text = ["国卷购买 \\d+(\\.\\d+)?[kKmMgGtTpPwW万亿]?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun buyBond(event: MessageEvent) {
        FoxBondUsecase.buyBond(event)
    }

    /**
     * 赎回国卷（不带ID赎回全部到期持仓，带ID赎回指定持仓）
     */
    @MessageAuthorize(text = ["国卷赎回( \\d+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun redeemBond(event: MessageEvent) {
        FoxBondUsecase.redeemBond(event)
    }

}
