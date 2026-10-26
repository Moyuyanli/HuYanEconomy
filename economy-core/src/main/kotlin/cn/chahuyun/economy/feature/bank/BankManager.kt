package cn.chahuyun.economy.feature.bank

import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.feature.user.UserManager
import cn.chahuyun.economy.model.bank.BankInfoDto
import cn.chahuyun.economy.runtime.EconomyRuntime
import cn.chahuyun.economy.scheduler.HuYanScheduler
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.FormatUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.ShareUtils
import cn.hutool.core.date.DateUtil

/**
 * 主银行对外管理器：初始化、定时任务、银行档案读写。
 *
 * 除本功能内的 usecase/规则外，`UserManager`（个人信息里的利率）与私银功能（利率基准）
 * 也会调用这里，因此按开发规则保留 Manager。
 */
object BankManager {

    /**
     * 初始化主银行。
     *
     * - 确保全局主银行存在。
     * - 启动银行利息定时任务。
     */
    @JvmStatic
    fun init() {
        val one = bankProxy.findById(1)
        if (one == null) {
            bankProxy.save(
                BankInfoDto(
                    code = "global",
                    name = "主银行",
                    description = "壶言经济主银行",
                    qq = EconomyRuntime.config.owner,
                    regTime = System.currentTimeMillis(),
                    regTotal = 0.0,
                    interestSwitch = true,
                    interest = BankInterestService.randomInterest()
                )
            )
        }

        val bankInfos = try {
            bankProxy.findAll()
        } catch (e: Exception) {
            Log.error("银行管理: 利息加载出错!", e)
            emptyList()
        }

        val bankInterestTask = BankInterestTask("bank", bankInfos)
        HuYanScheduler.schedule("bank", "0 0 4 * * ?", bankInterestTask)
    }

    private val bankProxy
        get() = EntityProxyRegistry.get<BankInfoDto>("bank") ?: error("银行代理器未初始化")

    fun getBankInfo(id: Long): BankInfoDto? = bankProxy.findById(id)

    fun saveBankInfo(bankInfo: BankInfoDto): BankInfoDto = bankProxy.save(bankInfo)
}

/**
 * 主银行利息定时任务（每天 04:00）。
 *
 * 两件事：
 * - 周二按 [BankInterestService.randomInterest] 刷新开启自动利率的银行；
 * - 给主银行（id=1）的每个存款账户按 `interest / 1000` 结息，并把本次利息写回用户资料。
 *
 * 原 `manager/BankTaskManager.kt` 只有这一个类、且只被 [BankManager.init] 使用，
 * 迁移时并入本文件，避免为单一定时任务保留独立文件。
 *
 * @property id 调度任务 id（日志里区分用）。
 * @property bankList 初始化时的银行快照。
 */
private class BankInterestTask(
    private val id: String,
    private val bankList: List<BankInfoDto>,
) : Runnable {

    override fun run() {
        for (bankInfo in bankList) {
            if (bankInfo.interestSwitch && DateUtil.thisDayOfWeek() == 2) {
                BankManager.saveBankInfo(bankInfo.copy(interest = BankInterestService.randomInterest()))
            }

            if (bankInfo.id == 1) {
                val interest = bankInfo.interest
                for (entry in EconomyUtil.bankEntries()) {
                    // 账本条目只带账户 ID（u<qq>），按 id 反查用户；查不到的账户直接跳过。
                    val userInfo = UserManager.getUserInfoByAccountId(entry.accountId) ?: continue
                    var v = ShareUtils.rounding(entry.amount.toDouble()) * (interest / 1000.0)
                    v = FormatUtil.round(v, 1)
                    if (EconomyUtil.plusMoneyToBankForAccount(entry.accountId, v)) {
                        userInfo.bankEarnings = v
                        UserManager.saveUserInfo(userInfo)
                    } else {
                        Log.error("银行利息管理:$id 添加利息出错")
                    }
                }
            }
        }
    }
}
