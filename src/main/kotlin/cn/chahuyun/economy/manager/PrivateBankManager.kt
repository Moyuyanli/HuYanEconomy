package cn.chahuyun.economy.manager

import cn.chahuyun.economy.privatebank.PrivateBankService
import cn.chahuyun.economy.utils.Log
import cn.hutool.cron.CronUtil
import cn.hutool.cron.task.Task

/**
 * 私人银行模块定时任务
 */
object PrivateBankManager {

    @JvmStatic
    fun init() {
        // 每日利息结算：给储户累积利息（简化实现，资金覆盖依赖主银行准备金池收益）
        CronUtil.schedule("private-bank-interest", "0 10 4 * * ?", PrivateBankInterestTask())

        // 每周国卷发行（确保每周至少存在一期）
        CronUtil.schedule("private-bank-bond-issue", "0 5 0 ? * MON", PrivateBankBondIssueTask())

        Log.info("私人银行模块已初始化")
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
                Log.error("私人银行:利息结算任务异常", e)
            }
        }
    }

    private class PrivateBankBondIssueTask : Task {
        override fun execute() {
            runCatching {
                PrivateBankService.ensureWeeklyBondIssue()
            }.onFailure { e ->
                Log.error("私人银行:国卷发行任务异常", e)
            }
        }
    }
}
