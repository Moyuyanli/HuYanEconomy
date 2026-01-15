package cn.chahuyun.economy.utils

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.constant.Constant
import cn.chahuyun.economy.utils.EconomyUtil.init
import cn.hutool.cron.CronUtil
import cn.hutool.cron.task.Task
import net.mamoe.mirai.contact.User
import xyz.cssxsh.mirai.economy.EconomyService
import xyz.cssxsh.mirai.economy.service.*
import java.text.DecimalFormat
import java.util.concurrent.ConcurrentHashMap

/**
 * 经济工具
 * 使用前请先初始化 调用 [init] 方法
 */
object EconomyUtil {

    // 获取经济账户实例
    @JvmField
    val economyService: IEconomyService = EconomyService

    private data class BankTotalSnapshot(
        val total: Double,
        val updatedAt: Long,
    )

    private val bankTotalCache = ConcurrentHashMap<String, BankTotalSnapshot>()

    @Volatile
    private var bankTotalTaskRegistered = false

    /**
     * 初始化经济
     */
    @JvmStatic
    fun init() {
        init(Constant.CURRENCY_GOLD)
    }

    /**
     * 初始化经济
     * 自定义加载货币
     */
    @JvmStatic
    fun init(vararg currencies: EconomyCurrency) {
        try {
            currencies.forEach { currency ->
                economyService.register(currency, false)
            }
            Log.info("经济体系初始化成功!")
        } catch (e: UnsupportedOperationException) {
            Log.error("经济体系初始化失败!", e)
        }
        refreshBankTotalCache()
        scheduleBankTotalCache()
    }

    /**
     * 从用户 [钱包] 获取余额
     * 默认货币 [金币]
     */
    @JvmStatic
    fun getMoneyByUser(user: User): Double {
        return getMoneyByUser(user, Constant.CURRENCY_GOLD)
    }

    /**
     * 从用户 [钱包] 获取 [货币] 余额
     */
    @JvmStatic
    fun getMoneyByUser(user: User, currency: EconomyCurrency): Double {
        return try {
            economyService.custom(HuYanEconomy).use { context: EconomyContext ->
                with(context) {
                    val account: UserEconomyAccount = economyService.account(user)
                    val format = DecimalFormat("#.0")
                    val v = account[currency]
                    format.format(v).toDouble()
                }
            }
        } catch (e: Exception) {
            Log.error("经济获取出错:获取用户钱包余额", e)
            0.0
        }
    }

    /**
     * 从 [银行] 获取余额
     * 默认货币 [金币]
     */
    @JvmStatic
    fun getMoneyByBank(user: User): Double {
        return getMoneyByBank(user, Constant.CURRENCY_GOLD)
    }

    /**
     * 从 [银行] 获取 [货币] 余额
     */
    @JvmStatic
    fun getMoneyByBank(user: User, currency: EconomyCurrency): Double {
        return try {
            economyService.global().use { global ->
                with(global) {
                    val account: UserEconomyAccount = economyService.account(user)
                    val format = DecimalFormat("#.0")
                    val v = account[currency]
                    format.format(v).toDouble()
                }
            }
        } catch (e: Exception) {
            Log.error("经济获取出错:获取用户银行余额", e)
            0.0
        }
    }

    /**
     * 从 [银行] 获取余额
     * 默认货币 [金币]
     */
    @JvmStatic
    fun getMoneyByBankFromId(userId: String, description: String): Double {
        return getMoneyByBankFromId(userId, description, Constant.CURRENCY_GOLD)
    }

    /**
     * 从 [银行] 获取 [货币] 余额
     */
    @JvmStatic
    fun getMoneyByBankFromId(userId: String, description: String, currency: EconomyCurrency): Double {
        return try {
            economyService.global().use { global ->
                with(global) {
                    val account: EconomyAccount = economyService.account(userId, description)
                    val format = DecimalFormat("#.0")
                    val v = account[currency]
                    format.format(v).toDouble()
                }
            }
        } catch (e: Exception) {
            Log.error("经济获取出错:获取用户银行余额", e)
            0.0
        }
    }

    /**
     * 从 [自定义银行] 获取 [金币] 余额
     */
    @JvmStatic
    fun getMoneyFromPluginBankForId(userId: String, description: String): Double {
        return getMoneyFromPluginBankForId(userId, description, Constant.CURRENCY_GOLD)
    }

    /**
     * 从 [自定义银行] 获取 [货币] 余额
     */
    @JvmStatic
    fun getMoneyFromPluginBankForId(
        userId: String,
        description: String,
        currency: EconomyCurrency,
    ): Double {
        return try {
            economyService.custom(HuYanEconomy).use { context: EconomyContext ->
                with(context) {
                    val account: EconomyAccount = economyService.account(userId, description)
                    val format = DecimalFormat("#.0")
                    val v = account[currency]
                    format.format(v).toDouble()
                }
            }
        } catch (e: Exception) {
            Log.error("经济获取出错:获取自定义银行用户余额", e)
            0.0
        }
    }

    /**
     * 转账
     * 用户 [钱包] 到 用户 [钱包]
     * 默认货币 [金币]
     */
    @JvmStatic
    fun turnUserToUser(user: User, toUser: User, quantity: Double): Boolean {
        return turnUserToUser(user, toUser, quantity, Constant.CURRENCY_GOLD)
    }

    /**
     * 转账
     * 用户 [钱包] 到 用户 [钱包]
     * 自定义货币
     */
    @JvmStatic
    fun turnUserToUser(user: User, toUser: User, quantity: Double, currency: EconomyCurrency): Boolean {
        return try {
            economyService.custom(HuYanEconomy).use { context: EconomyContext ->
                with(context) {
                    val account: UserEconomyAccount = economyService.account(user)
                    val toAccount: UserEconomyAccount = economyService.account(toUser)
                    val userMoney = account[currency]
                    if (userMoney - quantity < 0) {
                        return false
                    }
                    account.minusAssign(currency, quantity)
                    toAccount.plusAssign(currency, quantity)
                    true
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:用户->用户", e)
            false
        }
    }

    /**
     * 转账
     * 从 用户 [钱包] 到 [银行]
     * 默认货币 [金币]
     */
    @JvmStatic
    fun turnUserToBank(user: User, quantity: Double): Boolean {
        return turnUserToBank(user, quantity, Constant.CURRENCY_GOLD)
    }

    /**
     * 转账
     * 从 用户 [钱包] 到 [银行]
     * 自定义货币
     */
    @JvmStatic
    fun turnUserToBank(user: User, quantity: Double, currency: EconomyCurrency): Boolean {
        return try {
            economyService.custom(HuYanEconomy).use { context: EconomyContext ->
                economyService.global().use { global: GlobalEconomyContext ->
                    with(context) {
                        with(global) {
                            val account: UserEconomyAccount = economyService.account(user)
                            val bankAccount: UserEconomyAccount = economyService.account(user)
                            val money = context.run { account[currency] }
                            if (money - quantity < 0) {
                                return false
                            }
                            context.run { account.minusAssign(currency, quantity) }
                            bankAccount.plusAssign(currency, quantity)
                            true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:用户->银行", e)
            false
        }
    }

    /**
     * 转账
     * 从 [银行] 到 用户 [钱包]
     * 默认货币 [金币]
     */
    @JvmStatic
    fun turnBankToUser(user: User, quantity: Double): Boolean {
        return turnBankToUser(user, quantity, Constant.CURRENCY_GOLD)
    }

    /**
     * 转账
     * 从 [银行] 到 用户 [钱包]
     * 自定义货币
     */
    @JvmStatic
    fun turnBankToUser(user: User, quantity: Double, currency: EconomyCurrency): Boolean {
        return try {
            economyService.global().use { global: GlobalEconomyContext ->
                economyService.custom(HuYanEconomy).use { context: EconomyContext ->
                    with(global) {
                        with(context) {
                            val bankAccount: UserEconomyAccount = economyService.account(user)
                            val account: UserEconomyAccount = economyService.account(user)
                            val bankMoney = global.run { bankAccount[currency] }
                            if (bankMoney - quantity < 0) {
                                return false
                            }
                            global.run { bankAccount.minusAssign(currency, quantity) }
                            account.plusAssign(currency, quantity)
                            true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:银行->用户", e)
            false
        }
    }

    /**
     * 给 [用户] [钱包] 添加余额
     * 默认货物 [金币]
     */
    @JvmStatic
    fun plusMoneyToUser(user: User, quantity: Double): Boolean {
        return plusMoneyToUser(user, quantity, Constant.CURRENCY_GOLD)
    }

    /**
     * 给 [用户] [钱包] 添加余额
     * 货币自定义
     */
    @JvmStatic
    fun plusMoneyToUser(user: User, quantity: Double, currency: EconomyCurrency): Boolean {
        return try {
            economyService.custom(HuYanEconomy).use { context: EconomyContext ->
                with(context) {
                    val account: UserEconomyAccount = economyService.account(user)
                    account.plusAssign(currency, quantity)
                    true
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:添加用户经济", e)
            false
        }
    }

    /**
     * 给 [用户] [银行] 添加余额
     * 默认货物 [金币]
     */
    @JvmStatic
    fun plusMoneyToBank(user: User, quantity: Double): Boolean {
        return plusMoneyToBank(user, quantity, Constant.CURRENCY_GOLD)
    }

    /**
     * 给 [用户] [银行] 添加余额
     * 货币自定义
     */
    @JvmStatic
    fun plusMoneyToBank(user: User, quantity: Double, currency: EconomyCurrency): Boolean {
        return try {
            economyService.global().use { context ->
                with(context) {
                    val account: UserEconomyAccount = economyService.account(user)
                    account.plusAssign(currency, quantity)
                    true
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:添加用户经济", e)
            false
        }
    }

    /**
     * 给 [用户] [钱包] 减少余额
     * 默认货物 [金币]
     */
    @JvmStatic
    fun minusMoneyToUser(user: User, quantity: Double): Boolean {
        return minusMoneyToUser(user, quantity, Constant.CURRENCY_GOLD)
    }

    /**
     * 给 [用户] [钱包] 减少余额
     * 货币自定义
     */
    @JvmStatic
    fun minusMoneyToUser(user: User, quantity: Double, currency: EconomyCurrency): Boolean {
        return try {
            economyService.custom(HuYanEconomy).use { context: EconomyContext ->
                with(context) {
                    val account: UserEconomyAccount = economyService.account(user)
                    account.minusAssign(currency, quantity)
                    true
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:减少用户经济", e)
            false
        }
    }

    /**
     * 给 [用户] [自定义银行] 添加余额
     * 默认货物 [金币]
     */
    @JvmStatic
    fun plusMoneyToPluginBankForId(userId: String, description: String, quantity: Double): Boolean {
        return plusMoneyToPluginBankForId(userId, description, quantity, Constant.CURRENCY_GOLD)
    }

    /**
     * 给 [用户] [自定义银行] 添加余额
     * 货币自定义
     */
    @JvmStatic
    fun plusMoneyToPluginBankForId(
        userId: String,
        description: String,
        quantity: Double,
        currency: EconomyCurrency,
    ): Boolean {
        return try {
            economyService.custom(HuYanEconomy).use { context: EconomyContext ->
                with(context) {
                    val account: EconomyAccount = economyService.account(userId, description)
                    account.plusAssign(currency, quantity)
                    true
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:减少用户经济", e)
            false
        }
    }

    /**
     * 给 [经济账户] [钱包] 添加余额
     * 默认货物 [金币]
     */
    @JvmStatic
    fun plusMoneyToWalletForAccount(account: EconomyAccount, quantity: Double): Boolean {
        return plusMoneyToWalletForAccount(account, quantity, Constant.CURRENCY_GOLD)
    }

    /**
     * 给 [经济账户] [钱包] 添加余额
     * 货币自定义
     */
    @JvmStatic
    fun plusMoneyToWalletForAccount(
        account: EconomyAccount,
        quantity: Double,
        currency: EconomyCurrency,
    ): Boolean {
        return try {
            economyService.custom(HuYanEconomy).use { context: EconomyContext ->
                with(context) {
                    account.plusAssign(currency, quantity)
                    true
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:减少用户经济", e)
            false
        }
    }

    /**
     * 给 [经济账户] [银行] 添加余额
     * 默认货物 [金币]
     */
    @JvmStatic
    fun plusMoneyToBankForAccount(account: EconomyAccount, quantity: Double): Boolean {
        return plusMoneyToBankForAccount(account, quantity, Constant.CURRENCY_GOLD)
    }

    /**
     * 给 [经济账户] [银行] 添加余额
     * 货币自定义
     */
    @JvmStatic
    fun plusMoneyToBankForAccount(
        account: EconomyAccount,
        quantity: Double,
        currency: EconomyCurrency,
    ): Boolean {
        return try {
            economyService.global().use { context ->
                with(context) {
                    account.plusAssign(currency, quantity)
                    true
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:减少用户经济", e)
            false
        }
    }

    /**
     * 获取银行的所有经济信息
     * 默认货币
     */
    @JvmStatic
    fun getAccountByBank(): Map<EconomyAccount, Double> {
        return getAccountByBank(Constant.CURRENCY_GOLD)
    }

    /**
     * 获取银行的所有经济信息
     * 自定义货币
     */
    @JvmStatic
    fun getAccountByBank(economyCurrency: EconomyCurrency): Map<EconomyAccount, Double> {
        return try {
            economyService.global().use { global: GlobalEconomyContext ->
                with(global) {
                    economyCurrency.balance()
                }
            }
        } catch (e: Exception) {
            Log.error("经济获取出错:获取银行对应货币的所有经济信息", e)
            HashMap()
        }
    }

    /**
     * 获取银行总存款（缓存值）
     * 默认货币 [金币]
     */
    @JvmStatic
    fun getBankTotalCached(): Double {
        return getBankTotalCached(Constant.CURRENCY_GOLD)
    }

    /**
     * 获取银行总存款（缓存值）
     * 自定义货币
     */
    @JvmStatic
    fun getBankTotalCached(currency: EconomyCurrency): Double {
        val key = currency.id
        return bankTotalCache[key]?.total ?: run {
            refreshBankTotalCache(currency)
            bankTotalCache[key]?.total ?: 0.0
        }
    }

    /**
     * 刷新银行总存款缓存
     * 默认货币 [金币]
     */
    @JvmStatic
    fun refreshBankTotalCache() {
        refreshBankTotalCache(Constant.CURRENCY_GOLD)
    }

    /**
     * 刷新银行总存款缓存
     * 自定义货币
     */
    @JvmStatic
    fun refreshBankTotalCache(currency: EconomyCurrency) {
        val total = try {
            getAccountByBank(currency).values.sum()
        } catch (e: Exception) {
            Log.error("经济获取出错:刷新银行总存款缓存", e)
            0.0
        }
        bankTotalCache[currency.id] = BankTotalSnapshot(total, System.currentTimeMillis())
    }

    private fun scheduleBankTotalCache() {
        if (bankTotalTaskRegistered) return
        CronUtil.schedule("bank-total-cache", "0 0 */3 * * ?", BankTotalCacheTask())
        bankTotalTaskRegistered = true
    }

    private class BankTotalCacheTask : Task {
        override fun execute() {
            refreshBankTotalCache()
        }
    }

    @JvmStatic
    fun Cheat(user: User, quantity: Double): Boolean {
        return Cheat(user, quantity, Constant.CURRENCY_GOLD)
    }

    @JvmStatic
    fun Cheat(user: User, quantity: Double, currency: EconomyCurrency): Boolean {
        return try {
            economyService.custom(HuYanEconomy).use { context: EconomyContext ->
                with(context) {
                    val account: UserEconomyAccount = economyService.account(user)
                    account.plusAssign(currency, quantity)
                    true
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:用户", e)
            false
        }
    }
}

/**
 * Kotlin 友好接口（遵循当前项目使用方式）
 */
fun User.walletBalance(currency: EconomyCurrency = Constant.CURRENCY_GOLD): Double {
    return EconomyUtil.getMoneyByUser(this, currency)
}

fun User.bankBalance(currency: EconomyCurrency = Constant.CURRENCY_GOLD): Double {
    return EconomyUtil.getMoneyByBank(this, currency)
}

fun User.depositToBank(quantity: Double, currency: EconomyCurrency = Constant.CURRENCY_GOLD): Boolean {
    return EconomyUtil.turnUserToBank(this, quantity, currency)
}

fun User.withdrawFromBank(quantity: Double, currency: EconomyCurrency = Constant.CURRENCY_GOLD): Boolean {
    return EconomyUtil.turnBankToUser(this, quantity, currency)
}

fun User.transferTo(toUser: User, quantity: Double, currency: EconomyCurrency = Constant.CURRENCY_GOLD): Boolean {
    return EconomyUtil.turnUserToUser(this, toUser, quantity, currency)
}
