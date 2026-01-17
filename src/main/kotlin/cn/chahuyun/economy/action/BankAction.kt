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
     * 存款
     */
    @MessageAuthorize(text = ["存款 \\d+", "deposit \\d+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun deposit(event: MessageEvent) {
        BankUsecase.deposit(event)
    }

    /**
     * 主银行存款（兼容：当用户设置了默认私银后仍可直达主行）
     */
    @MessageAuthorize(text = ["主存款 \\d+", "main-deposit \\d+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun mainBankDeposit(event: MessageEvent) {
        BankUsecase.mainBankDeposit(event)
    }

    /**
     * 私银存款：存款 <金额> <code/name>
     */
    @MessageAuthorize(text = ["存款 \\d+(\\.\\d+)? \\S+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun privateBankDeposit(event: MessageEvent) {
        BankUsecase.privateBankDeposit(event)
    }

    /**
     * 一键存款：把钱包余额的整数部分全部存入主银行
     */
    @MessageAuthorize(text = ["存款!", "deposit!"])
    suspend fun depositAllInteger(event: MessageEvent) {
        BankUsecase.depositAllInteger(event)
    }

    /**
     * 取款
     */
    @MessageAuthorize(text = ["取款 \\d+", "withdraw \\d+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun withdrawal(event: MessageEvent) {
        BankUsecase.withdrawal(event)
    }

    /**
     * 主银行取款（兼容：当用户设置了默认私银后仍可直达主行）
     */
    @MessageAuthorize(text = ["主取款 \\d+", "main-withdraw \\d+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun mainBankWithdraw(event: MessageEvent) {
        BankUsecase.mainBankWithdraw(event)
    }

    /**
     * 私银取款：取款 <金额> <code/name>
     */
    @MessageAuthorize(text = ["取款 \\d+(\\.\\d+)? \\S+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun privateBankWithdraw(event: MessageEvent) {
        BankUsecase.privateBankWithdraw(event)
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
