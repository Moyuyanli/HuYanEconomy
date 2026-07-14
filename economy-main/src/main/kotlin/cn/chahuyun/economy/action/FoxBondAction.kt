package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
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
    @MessageAuthorize(text = ["国卷列表", "国卷"])
    suspend fun bondList(event: MessageEvent) {
        FoxBondUsecase.bondList(event)
    }

    @MessageAuthorize(text = ["国卷持仓"])
    suspend fun bondHoldings(event: MessageEvent) {
        FoxBondUsecase.bondHoldings(event)
    }

    @MessageAuthorize(text = ["国卷补发"], userPermissions = [AuthPerm.OWNER])
    suspend fun supplementBonds(event: MessageEvent) {
        FoxBondUsecase.supplementBonds(event)
    }

    /**
     * 购买国卷（行长用流动金池资金购买指定 code 的国卷）
     */
    @MessageAuthorize(text = ["国卷购买 \\d+(\\.\\d+)?[kKmMgGtTpPwWeE万亿]? \\S+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun buyBond(event: MessageEvent) {
        FoxBondUsecase.buyBond(event)
    }

    /**
     * 赎回国卷（不带金额赎回该 code 全部持仓，带金额赎回指定金额）
     */
    @MessageAuthorize(text = ["国卷赎回 \\S+( \\d+(\\.\\d+)?[kKmMgGtTpPwWeE万亿]?)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun redeemBond(event: MessageEvent) {
        FoxBondUsecase.redeemBond(event)
    }

}
