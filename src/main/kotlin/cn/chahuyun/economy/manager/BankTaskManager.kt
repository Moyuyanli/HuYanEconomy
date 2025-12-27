package cn.chahuyun.economy.manager

import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.entity.bank.BankInfo
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.ShareUtils
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.date.DateUtil
import cn.hutool.cron.task.Task
import xyz.cssxsh.mirai.economy.service.EconomyAccount

/**
 * 银行利息定时任务（从 `BankAction.java` 迁移）。
 */
class BankInterestTask(
    private val id: String,
    private val bankList: List<BankInfo>
) : Task {

    override fun execute() {
        for (bankInfo in bankList) {
            if (bankInfo.interestSwitch && DateUtil.thisDayOfWeek() == 2) {
                bankInfo.interest = BankInfo.randomInterest()
                HibernateFactory.merge(bankInfo)
            }

            if (bankInfo.id == 1) {
                val interest = bankInfo.interest
                val accountByBank: Map<EconomyAccount, Double> = EconomyUtil.getAccountByBank()
                for ((account, money) in accountByBank) {
                    val userInfo: UserInfo = UserCoreManager.getUserInfo(account)
                    var v = ShareUtils.rounding(money) * (interest / 1000.0)
                    v = String.format("%.1f", v).toDouble()
                    if (EconomyUtil.plusMoneyToBankForAccount(account, v)) {
                        userInfo.bankEarnings = v
                        HibernateFactory.merge(userInfo)
                    } else {
                        Log.error("银行利息管理:$id 添加利息出错")
                    }
                }
            }
        }
    }
}


