package cn.chahuyun.economy.manager

import cn.chahuyun.economy.privatebank.PrivateBankFoxBondService
import cn.chahuyun.economy.privatebank.PrivateBankService
import cn.chahuyun.economy.utils.Log
import cn.hutool.cron.CronUtil
import cn.hutool.cron.task.Task

/**
 * 银行（PrivateBank 模块）定时任务
 */
object PrivateBankManager {

    @JvmStatic
    fun init() {
        // 每日利息结算：给储户累积利息（简化实现，资金覆盖依赖主银行准备金池收益）
        CronUtil.schedule("private-bank-interest", "0 10 4 * * ?", PrivateBankInterestTask())

        // 到期贷款追缴：自动从借款人主银行/钱包扣款
        CronUtil.schedule("private-bank-loan-collect", "0 20 4 * * ?", PrivateBankLoanCollectTask())

        // 每周国卷发行（确保每周至少存在一期）
        CronUtil.schedule("private-bank-bond-issue", "0 5 0 ? * MON", PrivateBankBondIssueTask())

        // 狐卷（计划内竞标系统）：每月 1/15 发行 + 截标结算
        CronUtil.schedule("private-bank-foxbond-issue", "0 0 8 1,15 * ?", PrivateBankFoxBondIssueTask())
        CronUtil.schedule("private-bank-foxbond-settle", "0 5 18 1,15 * ?", PrivateBankFoxBondSettleTask())
        CronUtil.schedule("private-bank-foxbond-redeem", "0 15 4 * * ?", PrivateBankFoxBondRedeemTask())

        Log.info("银行模块已初始化")
    }

    private class PrivateBankInterestTask : Task {
        override fun execute() {
            runCatching {
                val banks = cn.chahuyun.economy.privatebank.PrivateBankRepository.listBanks()
                banks.forEach { cn.chahuyun.economy.privatebank.PrivateBankService.refreshRating(it.code) }

                // 利息累积放到 Service 内部逐银行执行，避免 action 层混入
                banks.forEach { bank ->
                    val base = java.util.Date()
                    // 逐储户累积利息
                    val deposits = cn.chahuyun.economy.privatebank.PrivateBankRepository.listDeposits(bank.code)
                    if (deposits.isEmpty()) return@forEach

                    val rate = if (bank.isDefaulter()) {
                        // 失信期间利率强制同步主银行（已在取款失败时设置）
                        bank.depositorInterest
                    } else {
                        bank.depositorInterest
                    }

                    deposits.forEach { dep ->
                        val delta = cn.chahuyun.economy.utils.ShareUtils.rounding(dep.principal * (rate / 1000.0))
                        if (delta <= 0) return@forEach
                        dep.principal = cn.chahuyun.economy.utils.ShareUtils.rounding(dep.principal + delta)
                        dep.updatedAt = base
                        cn.chahuyun.economy.privatebank.PrivateBankRepository.saveDeposit(dep)
                    }
                }
            }.onFailure { e ->
                Log.error("银行:利息结算任务异常", e)
            }
        }
    }

    private class PrivateBankBondIssueTask : Task {
        override fun execute() {
            runCatching {
                PrivateBankService.ensureWeeklyBondIssue()
            }.onFailure { e ->
                Log.error("银行:国卷发行任务异常", e)
            }
        }
    }

    private class PrivateBankFoxBondIssueTask : Task {
        override fun execute() {
            runCatching {
                val bonds = PrivateBankFoxBondService.issueBondsForToday()
                if (bonds.isNotEmpty()) {
                    Log.info("银行:狐卷已发行 ${bonds.size} 张")
                }
            }.onFailure { e ->
                Log.error("银行:狐卷发行任务异常", e)
            }
        }
    }

    private class PrivateBankFoxBondSettleTask : Task {
        override fun execute() {
            runCatching {
                val n = PrivateBankFoxBondService.settleExpiredBids()
                if (n > 0) {
                    Log.info("银行:狐卷结算完成 $n 张")
                }
            }.onFailure { e ->
                Log.error("银行:狐卷结算任务异常", e)
            }
        }
    }

    private class PrivateBankFoxBondRedeemTask : Task {
        override fun execute() {
            runCatching {
                val n = PrivateBankFoxBondService.redeemMaturedHoldings()
                if (n > 0) {
                    Log.info("银行:狐卷到期回流 $n 笔")
                }
            }.onFailure { e ->
                Log.error("银行:狐卷到期回流任务异常", e)
            }
        }
    }

    private class PrivateBankLoanCollectTask : Task {
        override fun execute() {
            runCatching {
                val n = PrivateBankService.collectOverdueLoans()
                if (n > 0) {
                    Log.info("银行:到期贷款已处理 $n 笔")
                }
            }.onFailure { e ->
                Log.error("银行:到期贷款追缴任务异常", e)
            }
        }
    }
}
