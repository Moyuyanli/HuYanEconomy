package cn.chahuyun.economy.feature.privatebank

import cn.chahuyun.economy.data.repository.PrivateBankRepository
import cn.chahuyun.economy.feature.privatebank.PrivateBankManager.init
import cn.chahuyun.economy.feature.privatebank.PrivateBankManager.ownedBank
import cn.chahuyun.economy.model.privatebank.PrivateBankDepositDto
import cn.chahuyun.economy.model.privatebank.PrivateBankDto
import cn.chahuyun.economy.model.privatebank.PrivateBankGovBondIssueDto
import cn.chahuyun.economy.scheduler.HuYanScheduler
import cn.chahuyun.economy.utils.Log
import net.mamoe.mirai.contact.User
import java.util.Date

/**
 * 私银功能的对外管理器：初始化 + 定时任务 + 跨功能查询入口。
 *
 * 本功能的指令入口在 [PrivateBankUsecase]，规则链在 [PrivateBankRules]；
 * 这里保留 Manager 是因为**功能之外**有调用方：
 * - 插件启动（`HuYanEconomy.onEnable`）要 [init] 注册 7 个定时任务；
 * - 国卷功能（`feature/bond`）要按 QQ 找到行长的银行，走 [ownedBank]；
 * - 主银行（`feature/bank`）、国卷（`feature/bond`）、转账（`feature/transfer`）、
 *   抢劫（`games/rob`）要用存取款 / 贷款 / 国卷能力，走下方「跨功能 API」段。
 *
 * 按 §3.3.6，跨 feature 只允许调用 Manager，不直连 [PrivateBankService]：
 * 下方的转发方法就是对外契约，内部实现全部留在 [PrivateBankService]。
 */
object PrivateBankManager {

    /**
     * 查询某个 QQ 作为行长拥有的私银。
     *
     * 跨功能只允许调用 Manager，不允许直接调 Usecase 或 Service，
     * 因此国卷功能通过本方法拿银行 code，而不是自己去查 repository。
     *
     * @param qq 行长的 QQ 号。
     * @return 该用户拥有的银行；没有返回 `null`。
     */
    fun ownedBank(qq: Long): PrivateBankDto? =
        PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == qq }

    // ===== 跨功能 API（对外契约，实现在 PrivateBankService）=====

    /** 按编码或名称查私银。`feature/bank` 利率与存取场景使用。 */
    fun getBank(key: String): PrivateBankDto? = PrivateBankService.getBank(key)

    /** 查某用户在指定私银的存款。`feature/bank` 存款展示使用。 */
    fun getDeposit(bankCode: String, userQq: Long): PrivateBankDepositDto? =
        PrivateBankService.getDeposit(bankCode, userQq)

    /** 向私银存款。`feature/bank` 存款动作使用。 */
    fun deposit(user: User, bankCode: String, amount: Double): Pair<Boolean, String> =
        PrivateBankService.deposit(user, bankCode, amount)

    /** 从私银取款。`feature/bank` 取款动作使用。 */
    fun withdraw(user: User, bankCode: String, amount: Double): Pair<Boolean, String> =
        PrivateBankService.withdraw(user, bankCode, amount)

    /** 用户是否有未还清贷款。`feature/transfer` 与 `games/rob` 的资格校验使用。 */
    fun hasUnrepaidLoans(userQq: Long): Boolean = PrivateBankService.hasUnrepaidLoans(userQq)

    /** 用一笔收入优先偿还逾期贷款。`games/rob` 抢劫结算使用。 */
    fun repayOverdueLoansFromIncome(userQq: Long, income: Double): PrivateBankService.LoanIncomeRepayment =
        PrivateBankService.repayOverdueLoansFromIncome(userQq, income)

    /** 确保当日国卷已发行（不足补足）。`feature/bond` 与定时任务使用。 */
    fun ensureDailyBondIssues(now: Date = Date()): List<PrivateBankGovBondIssueDto> =
        PrivateBankService.ensureDailyBondIssues(now)

    /** 手工补发当日国卷。`feature/bond` 补发指令使用。 */
    fun supplementDailyBondIssues(now: Date = Date()): List<PrivateBankGovBondIssueDto> =
        PrivateBankService.supplementDailyBondIssues(now)

    /** 列出当前可购买的国卷。`feature/bond` 列表指令使用。 */
    fun listAvailableBondIssues(now: Date = Date()): List<PrivateBankGovBondIssueDto> =
        PrivateBankService.listAvailableBondIssues(now)

    /** 计算国卷赎回时间戳。`feature/bond` 展示使用。 */
    fun bondRedeemAt(issue: PrivateBankGovBondIssueDto): Long = PrivateBankService.bondRedeemAt(issue)

    /** 购买国卷。`feature/bond` 购买动作使用。 */
    fun buyBond(owner: User, bankCode: String, bondCode: String, amount: Double): Pair<Boolean, String> =
        PrivateBankService.buyBond(owner, bankCode, bondCode, amount)

    /** 赎回国卷（按金额或全额）。`feature/bond` 赎回动作使用。 */
    fun redeemBond(owner: User, bondCode: String, requestedAmount: Double? = null): Pair<Boolean, String> =
        PrivateBankService.redeemBond(owner, bondCode, requestedAmount)

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
                cn.chahuyun.economy.feature.privatebank.PrivateBankDebtService.accrueAll()
                val bankruptcies = cn.chahuyun.economy.feature.privatebank.PrivateBankBankruptcyService.processEligibleBanks()
                if (bankruptcies > 0) Log.warning("银行:完成破产清算 $bankruptcies 家")
                banks.forEach { cn.chahuyun.economy.feature.privatebank.PrivateBankService.refreshRating(it.code) }

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
