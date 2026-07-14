package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.usecase.BankUsecase
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 银行管理
 * 存款 | 取款 | 富豪榜
 */
@EventComponent
class BankAction {

    /**
     * 存款：统一处理默认银行、主银行和指定私银。
     */
    @MessageAuthorize(
        text = ["(存款|deposit)(!{1,2}| \\S+( \\S+)?)"],
        messageMatching = MessageMatchingEnum.REGULAR
    )
    suspend fun deposit(event: MessageEvent) {
        BankUsecase.deposit(event)
    }

    /**
     * 取款：统一处理默认银行、主银行和指定私银。
     */
    @MessageAuthorize(
        text = ["(取款|withdraw)(!{1,2}| \\S+( \\S+)?)"],
        messageMatching = MessageMatchingEnum.REGULAR
    )
    suspend fun withdrawal(event: MessageEvent) {
        BankUsecase.withdrawal(event)
    }

    /**
     * 查看利率
     */
    @MessageAuthorize(text = ["本周利率", "银行利率"])
    suspend fun viewBankInterest(event: MessageEvent) {
        BankUsecase.viewBankInterest(event)
    }

    /**
     * 富豪榜
     */
    @MessageAuthorize(text = ["富豪榜", "经济排行"])
    suspend fun viewRegalTop(event: MessageEvent) {
        BankUsecase.viewRegalTop(event)
    }
}
