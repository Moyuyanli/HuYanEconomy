package cn.chahuyun.economy.manager

import cn.chahuyun.economy.privatebank.PrivateBankFoxBondService
import cn.chahuyun.economy.privatebank.PrivateBankService
import cn.chahuyun.economy.scheduler.HuYanScheduler
import cn.chahuyun.economy.utils.Log

/**
 * 银行（PrivateBank 模块）定时任务
 */
object PrivateBankManager {

    @JvmStatic
    fun init() {
        // 每日利息结算：给储户累积利息（简化实现，资金覆盖依赖主银行准备金池收益）
        HuYanScheduler.schedule("private-bank-interest", "0 10 4 * * ?", PrivateBankInterestTask())

        // 到期贷款追缴：自动从借款人主银行/钱包扣款
        HuYanScheduler.schedule("private-bank-loan-collect", "0 20 4 * * ?", PrivateBankLoanCollectTask())

        // 每天 10:30 发布当日国卷。
        HuYanScheduler.schedule("private-bank-bond-issue", "0 30 10 * * ?", PrivateBankBondIssueTask())
        HuYanScheduler.schedule("private-bank-bond-redeem", "0 10 4 * * ?", PrivateBankBondRedeemTask())

        // 狐卷（计划内竞标系统）：每月 1/15 发行 + 截标结算
        HuYanScheduler.schedule("private-bank-foxbond-issue", "0 0 8 1,15 * ?", PrivateBankFoxBondIssueTask())
        HuYanScheduler.schedule("private-bank-foxbond-settle", "0 5 18 1,15 * ?", PrivateBankFoxBondSettleTask())
        HuYanScheduler.schedule("private-bank-foxbond-redeem", "0 15 4 * * ?", PrivateBankFoxBondRedeemTask())

        Log.info("银行模块已初始化")
    }

    private class PrivateBankInterestTask : Runnable {
        override fun run() {
            runCatching {
                val banks = cn.chahuyun.economy.data.repository.PrivateBankRepository.listBanks()
                cn.chahuyun.economy.privatebank.PrivateBankDebtService.accrueAll()
                val bankruptcies = cn.chahuyun.economy.privatebank.PrivateBankBankruptcyService.processEligibleBanks()
                if (bankruptcies > 0) Log.warning("银行:完成破产清算 $bankruptcies 家")
                banks.forEach { cn.chahuyun.economy.privatebank.PrivateBankService.refreshRating(it.code) }

                // 利息累积放到 Service 内部逐银行执行，避免 action 层混入
                banks.forEach bankLoop@{ bank ->
                    val currentBank = cn.chahuyun.economy.data.repository.PrivateBankRepository.findBankByCode(bank.code)
                        ?: return@bankLoop
                    if (currentBank.bankruptAt > 0) return@bankLoop
                    val base = System.currentTimeMillis()
                    // 逐储户累积利息
                    val deposits = cn.chahuyun.economy.data.repository.PrivateBankRepository.listDeposits(bank.code)
                    if (deposits.isEmpty()) return@bankLoop

                    // 失信期间利率已在取款失败时强制同步主银行，此处统一使用 depositorInterest
                    val rate = currentBank.depositorInterest

                    deposits.forEach depositLoop@{ dep ->
                        val delta = cn.chahuyun.economy.utils.ShareUtils.rounding(dep.principal * (rate / 1000.0))
                        if (delta <= 0) return@depositLoop
                        dep.principal = cn.chahuyun.economy.utils.ShareUtils.rounding(dep.principal + delta)
                        dep.updatedAt = base
                        cn.chahuyun.economy.data.repository.PrivateBankRepository.saveDeposit(dep)
                    }
                }
            }.onFailure { e ->
                Log.error("银行:利息结算任务异常", e)
            }
        }
    }

    private class PrivateBankBondIssueTask : Runnable {
        override fun run() {
            runCatching {
                val issues = PrivateBankService.ensureDailyBondIssues()
                if (issues.isNotEmpty()) {
                    Log.info("银行:国卷已发行 ${issues.size} 份")
                }
            }.onFailure { e ->
                Log.error("银行:国卷发行任务异常", e)
            }
        }
    }

    private class PrivateBankBondRedeemTask : Runnable {
        override fun run() {
            runCatching {
                val n = PrivateBankService.redeemMaturedBondHoldings()
                if (n > 0) {
                    Log.info("银行:国卷到期回流 $n 笔")
                }
            }.onFailure { e ->
                Log.error("银行:国卷到期回流任务异常", e)
            }
        }
    }

    private class PrivateBankFoxBondIssueTask : Runnable {
        override fun run() {
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

    private class PrivateBankFoxBondSettleTask : Runnable {
        override fun run() {
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

    private class PrivateBankFoxBondRedeemTask : Runnable {
        override fun run() {
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

    private class PrivateBankLoanCollectTask : Runnable {
        override fun run() {
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
