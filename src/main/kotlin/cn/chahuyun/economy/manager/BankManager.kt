package cn.chahuyun.economy.manager

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.bank.BankInfoDto
import cn.chahuyun.economy.scheduler.HuYanScheduler
import cn.chahuyun.economy.utils.Log

/**
 * 涓婚摱琛屽垵濮嬪寲涓庡畾鏃朵换鍔＄鐞嗐€? */
object BankManager {

    /**
     * 鍒濆鍖栦富閾惰銆?
     * - 纭繚鍏ㄥ眬涓婚摱琛屽瓨鍦?
     * - 鍚姩閾惰鍒╂伅瀹氭椂浠诲姟
     */
    @JvmStatic
    fun init() {
        val one = bankProxy.findById(1)
        if (one == null) {
            bankProxy.save(
                BankInfoDto(
                    code = "global",
                    name = "主银行",
                    description = "缁忔祹鏈嶅姟",
                    qq = HuYanEconomy.config.owner,
                    regTime = System.currentTimeMillis(),
                    regTotal = 0.0,
                    interestSwitch = true,
                    interest = BankInfoDto.randomInterest()
                )
            )
        }

        val bankInfos = try {
            bankProxy.findAll()
        } catch (e: Exception) {
            Log.error("閾惰绠＄悊:鍒╂伅鍔犺浇鍑洪敊!", e)
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
