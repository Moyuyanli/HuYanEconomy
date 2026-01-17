package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.usecase.PrivateBankUsecase
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
        PrivateBankUsecase.listBanks(event)
    }

    @MessageAuthorize(text = ["银行创建 \\S+ .+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbCreate(event: MessageEvent) {
        PrivateBankUsecase.pbCreate(event)
    }

    @MessageAuthorize(text = ["银行描述修改"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbDesc(event: MessageEvent) {
        PrivateBankUsecase.pbDesc(event)
    }

    @MessageAuthorize(text = ["银行利率[变更|修改] \\S+"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbRate(event: MessageEvent) {
        PrivateBankUsecase.pbRate(event)
    }

    @MessageAuthorize(text = ["银行放贷 \\d+(\\.\\d+)? \\d+(\\.\\d+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbLoanOffer(event: MessageEvent) {
        PrivateBankUsecase.pbLoanOffer(event)
    }

    @MessageAuthorize(text = ["狐卷( 查看)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun foxView(event: MessageEvent) {
        PrivateBankUsecase.foxView(event)
    }

    @MessageAuthorize(
        text = ["狐卷竞标 \\S+ \\d+(\\.\\d+)? \\d+(\\.\\d+)?"], messageMatching = MessageMatchingEnum.REGULAR
    )
    suspend fun foxBid(event: MessageEvent) {
        PrivateBankUsecase.foxBid(event)
    }

    @MessageAuthorize(text = ["银行信息( \\S+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbInfo(event: MessageEvent) {
        PrivateBankUsecase.pbInfo(event)
    }

    @MessageAuthorize(text = ["银行评分 [1-5]( .+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbReview(event: MessageEvent) {
        PrivateBankUsecase.pbReview(event)
    }

    @MessageAuthorize(text = ["(贷款|借款) \\d+(\\.\\d+)?( \\S+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbBorrow(event: MessageEvent) {
        PrivateBankUsecase.pbBorrow(event)
    }

    @MessageAuthorize(text = ["还款 \\d+(\\.\\d+)?( \\S+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun pbRepay(event: MessageEvent) {
        PrivateBankUsecase.pbRepay(event)
    }


}
