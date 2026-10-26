package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.feature.privatebank.PrivateBankUsecaseActions
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 银行（PrivateBank 模块）指令入口
 *
 * 说明：
 * - 代码层模块名仍为 PrivateBank，但用户侧统一称为“银行”。
 * - 本模块尽量不影响现有主银行(存款/取款)逻辑。
 */
@EventComponent
class PrivateBankAction {
    @MessageAuthorize(text = ["银行列表"])
    suspend fun listBanks(event: MessageEvent) {
        PrivateBankUsecaseActions.listBanks(event)
    }

    @MessageAuthorize(text = ["默认银行"])
    suspend fun defaultBank(event: MessageEvent) {
        PrivateBankUsecaseActions.defaultBank(event)
    }

    @MessageAuthorize(text = ["默认银行设置 .+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun setDefaultBank(event: MessageEvent) {
        PrivateBankUsecaseActions.setDefaultBank(event)
    }

    @MessageAuthorize(text = ["银行创建 \\S+ .+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbCreate(event: MessageEvent) {
        PrivateBankUsecaseActions.pbCreate(event)
    }

    @MessageAuthorize(text = ["银行描述修改"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbDesc(event: MessageEvent) {
        PrivateBankUsecaseActions.pbDesc(event)
    }

    @MessageAuthorize(text = ["银行利率(变更|修改|调整) \\S+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbRate(event: MessageEvent) {
        PrivateBankUsecaseActions.pbRate(event)
    }

    @MessageAuthorize(text = ["银行补资 .+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbFund(event: MessageEvent) {
        PrivateBankUsecaseActions.pbFund(event)
    }

    @MessageAuthorize(text = ["银行撤资 \\d+(\\.\\d+)?[kKmMgGtTpPwWeE万亿]?( [pPfF])?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbDivest(event: MessageEvent) {
        PrivateBankUsecaseActions.pbDivest(event)
    }

    @MessageAuthorize(text = ["银行资料修改 \\S+ .+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbProfile(event: MessageEvent) {
        PrivateBankUsecaseActions.pbProfile(event)
    }

    @MessageAuthorize(
        text = ["(银行)?放贷 \\d+(\\.\\d+)?[kKmMgGtTpPwWeE万亿]?( \\d+(\\.\\d+)?)?"],
        messageMatching = MessageMatchingEnum.REGULAR
    )
    suspend fun pbLoanOffer(event: MessageEvent) {
        PrivateBankUsecaseActions.pbLoanOffer(event)
    }

    @MessageAuthorize(
        text = ["贷款利息(修改|变更) \\d+ \\d+(\\.\\d+)?", "放贷利息(修改|变更) \\d+ \\d+(\\.\\d+)?"],
        messageMatching = MessageMatchingEnum.REGULAR
    )
    suspend fun pbLoanRate(event: MessageEvent) {
        PrivateBankUsecaseActions.pbLoanRate(event)
    }

    @MessageAuthorize(text = ["放贷列表"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbLoanOffers(event: MessageEvent) {
        PrivateBankUsecaseActions.pbLoanOffers(event)
    }

    @MessageAuthorize(text = ["借贷列表"])
    suspend fun pbLoans(event: MessageEvent) {
        PrivateBankUsecaseActions.pbBorrowedLoans(event)
    }

    @MessageAuthorize(text = ["撤贷 \\d+", "贷款撤回 \\d+", "放贷撤回 \\d+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbLoanCancel(event: MessageEvent) {
        PrivateBankUsecaseActions.pbLoanCancel(event)
    }

    @MessageAuthorize(text = ["狐卷", "狐卷查看", "狐卷列表"])
    suspend fun foxView(event: MessageEvent) {
        PrivateBankUsecaseActions.foxView(event)
    }

    @MessageAuthorize(text = ["狐卷补发"], userPermissions = [AuthPerm.OWNER])
    suspend fun foxSupplement(event: MessageEvent) {
        PrivateBankUsecaseActions.foxSupplement(event)
    }

    @MessageAuthorize(
        text = ["狐卷竞标 \\S+ \\d+(\\.\\d+)?[kKmMgGtTpPwWeE万亿]? \\d+(\\.\\d+)?"], messageMatching = MessageMatchingEnum.REGULAR
    )
    suspend fun foxBid(event: MessageEvent) {
        PrivateBankUsecaseActions.foxBid(event)
    }

    @MessageAuthorize(text = ["银行信息( \\S+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbInfo(event: MessageEvent) {
        PrivateBankUsecaseActions.pbInfo(event)
    }

    @MessageAuthorize(text = ["我的银行"])
    suspend fun myBank(event: MessageEvent) {
        PrivateBankUsecaseActions.myBank(event)
    }

    @MessageAuthorize(text = ["我的银行#"])
    suspend fun myBankText(event: MessageEvent) {
        PrivateBankUsecaseActions.myBankText(event)
    }

    @MessageAuthorize(text = ["银行评分 [1-5]( .+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbReview(event: MessageEvent) {
        PrivateBankUsecaseActions.pbReview(event)
    }

    @MessageAuthorize(text = ["(贷款|借款) \\d+(\\.\\d+)?[kKmMgGtTpPwWeE万亿]?( \\S+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbBorrow(event: MessageEvent) {
        PrivateBankUsecaseActions.pbBorrow(event)
    }

    @MessageAuthorize(text = ["还款 \\d+(\\.\\d+)?[kKmMgGtTpPwWeE万亿]?( \\S+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbRepay(event: MessageEvent) {
        PrivateBankUsecaseActions.pbRepay(event)
    }


}
