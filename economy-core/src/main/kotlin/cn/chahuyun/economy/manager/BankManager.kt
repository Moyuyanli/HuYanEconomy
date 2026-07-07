package cn.chahuyun.economy.manager

import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.bank.BankInfoDto
import cn.chahuyun.economy.runtime.EconomyRuntime
import cn.chahuyun.economy.scheduler.HuYanScheduler
import cn.chahuyun.economy.service.BankInterestService
import cn.chahuyun.economy.utils.Log

/**
 * 主银行初始化与定时任务管理。
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
