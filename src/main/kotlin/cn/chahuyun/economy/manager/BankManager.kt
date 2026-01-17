package cn.chahuyun.economy.manager

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.entity.bank.BankInfo
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.cron.CronUtil

/**
 * 主银行初始化与定时任务管理。
 */
object BankManager {

    /**
     * 初始化主银行。
     * - 确保全局主银行存在
     * - 启动银行利息定时任务
     */
    @JvmStatic
    fun init() {
        val one = HibernateFactory.selectOneById(BankInfo::class.java, 1)
        if (one == null) {
            val bankInfo = BankInfo(
                "global",
                "主银行",
                "经济服务",
                HuYanEconomy.config.owner,
                0.0
            )
            HibernateFactory.merge(bankInfo)
        }

        val bankInfos = try {
            HibernateFactory.selectList(BankInfo::class.java)
        } catch (e: Exception) {
            Log.error("银行管理:利息加载出错!", e)
            emptyList()
        }

        val bankInterestTask = BankInterestTask("bank", bankInfos)
        CronUtil.schedule("bank", "0 0 4 * * ?", bankInterestTask)
    }
}
