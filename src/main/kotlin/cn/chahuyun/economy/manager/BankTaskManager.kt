package cn.chahuyun.economy.manager

import cn.chahuyun.economy.model.bank.BankInfoDto
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.FormatUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.ShareUtils
import cn.hutool.core.date.DateUtil
import xyz.cssxsh.mirai.economy.service.EconomyAccount

/**
 * 银行利息定时任务（从 `BankAction.java` 迁移）。
 */
class BankInterestTask(
    private val id: String,
    private val bankList: List<BankInfoDto>
) : Runnable {

    override fun run() {
        for (bankInfo in bankList) {
            if (bankInfo.interestSwitch && DateUtil.thisDayOfWeek() == 2) {
                BankManager.saveBankInfo(bankInfo.copy(interest = BankInfoDto.randomInterest()))
            }

            if (bankInfo.id == 1) {
                val interest = bankInfo.interest
                val accountByBank: Map<EconomyAccount, Double> = EconomyUtil.getAccountByBank()
                for ((account, money) in accountByBank) {
                    val userInfo= UserCoreManager.getUserInfo(account)
                    var v = ShareUtils.rounding(money) * (interest / 1000.0)
                    v = FormatUtil.round(v, 1)
                    if (EconomyUtil.plusMoneyToBankForAccount(account, v)) {
                        userInfo.bankEarnings = v
                        UserCoreManager.saveUserInfo(userInfo)
                    } else {
                        Log.error("银行利息管理:$id 添加利息出错")
                    }
                }
            }
        }
    }
}



