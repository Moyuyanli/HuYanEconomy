package cn.chahuyun.economy.ledger

import cn.chahuyun.economy.model.currency.GoldEconomyCurrency
import cn.chahuyun.economy.runtime.EconomyRuntime
import cn.chahuyun.economy.utils.Log
import net.mamoe.mirai.contact.User
import xyz.cssxsh.mirai.economy.EconomyService
import xyz.cssxsh.mirai.economy.service.*

/**
 * 上游 mirai-economy 的原始资金实现。
 *
 * 这是 2.0 之前 `EconomyUtil` 的实现体原样搬迁，**只允许** [MiraiEconomyLedger] 调用：
 * 本文件是插件里唯一直接依赖 `xyz.cssxsh.mirai.economy` 的资金实现处，上游插件未安装时
 * 不能触碰本文件的任何成员（否则会抛 `NoClassDefFoundError`）。
 *
 * 业务代码请使用 `EconomyUtil` 门面或 [cn.chahuyun.economy.base.ledger.EconomyLedger]。
 *
 * mirai-economy 区分 custom/global 两类上下文：
 * - custom(EconomyRuntime.plugin)：插件私有账户。**当前布局只用这个上下文**，
 *   scope 由 [MiraiEconomyLedger] 编码进账户 id 后缀。
 * - global()：全局银行账户。仅 [MiraiEconomyLedger.Layout.LEGACY_CONTEXT]（迁移读取
 *   2.0.1 之前的存量）会用到；运行时路径不再触碰。
 *
 * 文件后半部分的 `getMoneyByBank` / `turnUserToBank` 等方法是 2.0 之前的原始实现，
 * 保留作为历史布局的语义参考，**运行时已无调用方**（门面统一走 [EconomyLedger]）。
 */
internal object LegacyEconomyApi {

    /** 插件默认货币：金币。上游类型，不得出现在 economy-base 或业务层。 */
    @JvmField
    val CURRENCY_GOLD: EconomyCurrency = GoldEconomyCurrency()


    /** mirai-economy 服务入口，所有账户读写最终都从这里进入。 */
    @JvmField
    val economyService: IEconomyService = EconomyService

    /**
     * 初始化默认金币货币。
     */
    @JvmStatic
    fun init() {
        init(CURRENCY_GOLD)
    }

    /**
     * 初始化经济系统并注册货币。
     *
     * 只做货币注册。2.0.1 之前这里还会刷新 global 上下文的银行总额缓存并挂一个
     * 3 小时定时任务；改为「单上下文 + scope 后缀」布局后主银行已不在 global，
     * 那个缓存与任务全部失去意义，且会在每次启动做一次无用的全量扫描，故移除。
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
    }

    /**
     * 从用户 [钱包] 获取默认金币余额。
     */
    @JvmStatic
    fun getMoneyByUser(user: User): Double {
        return getMoneyByUser(user, CURRENCY_GOLD)
    }

    /**
     * 从用户 [钱包] 获取指定 [货币] 余额。
     *
     * 钱包余额存放在插件 custom 上下文，和 global 银行余额分开。
     */
    @JvmStatic
    fun getMoneyByUser(user: User, currency: EconomyCurrency): Double {
        return try {
            economyService.custom(EconomyRuntime.plugin).use { context: EconomyContext ->
                with(context) {
                    val account: UserEconomyAccount = economyService.account(user)
                    account[currency]
                }
            }
        } catch (e: Exception) {
            Log.error("经济获取出错:获取用户钱包余额", e)
            0.0
        }
    }

    /**
     * 从用户 [银行] 获取默认金币余额。
     */
    @JvmStatic
    fun getMoneyByBank(user: User): Double {
        return getMoneyByBank(user, CURRENCY_GOLD)
    }

    /**
     * 从用户 [银行] 获取指定 [货币] 余额。
     *
     * 银行余额走 global 上下文，因此和钱包账户不是同一资金池。
     */
    @JvmStatic
    fun getMoneyByBank(user: User, currency: EconomyCurrency): Double {
        return try {
            economyService.global().use { global ->
                with(global) {
                    val account: UserEconomyAccount = economyService.account(user)
                    account[currency]
                }
            }
        } catch (e: Exception) {
            Log.error("经济获取出错:获取用户银行余额", e)
            0.0
        }
    }

    /**
     * 从全局银行账户中按业务 id 和描述获取默认金币余额。
     */
    @JvmStatic
    fun getMoneyByBankFromId(userId: String, description: String): Double {
        return getMoneyByBankFromId(userId, description, CURRENCY_GOLD)
    }

    /**
     * 从全局银行账户中按业务 id 和描述获取指定 [货币] 余额。
     *
     * 私人银行准备金等需要全局可追踪的资金使用该路径。
     */
    @JvmStatic
    fun getMoneyByBankFromId(userId: String, description: String, currency: EconomyCurrency): Double {
        return try {
            economyService.global().use { global ->
                with(global) {
                    val account: EconomyAccount = economyService.account(userId, description)
                    account[currency]
                }
            }
        } catch (e: Exception) {
            Log.error("经济获取出错:获取用户银行余额", e)
            0.0
        }
    }

    /**
     * 从插件自定义账户中按业务 id 和描述获取金币余额。
     */
    @JvmStatic
    fun getMoneyFromPluginBankForId(userId: String, description: String): Double {
        return getMoneyFromPluginBankForId(userId, description, CURRENCY_GOLD)
    }

    /**
     * 从插件自定义账户中按业务 id 和描述获取指定 [货币] 余额。
     *
     * 私人银行流动金、库存池等插件内部资金使用该路径。
     */
    @JvmStatic
    fun getMoneyFromPluginBankForId(
        userId: String,
        description: String,
        currency: EconomyCurrency,
    ): Double {
        return try {
            economyService.custom(EconomyRuntime.plugin).use { context: EconomyContext ->
                with(context) {
                    val account: EconomyAccount = economyService.account(userId, description)
                    account[currency]
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
        return turnUserToUser(user, toUser, quantity, CURRENCY_GOLD)
    }

    /**
     * 转账
     * 用户 [钱包] 到 用户 [钱包]
     * 自定义货币
     */
    @JvmStatic
    fun turnUserToUser(user: User, toUser: User, quantity: Double, currency: EconomyCurrency): Boolean {
        return try {
            economyService.custom(EconomyRuntime.plugin).use { context: EconomyContext ->
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
        return turnUserToBank(user, quantity, CURRENCY_GOLD)
    }

    /**
     * 转账
     * 从 用户 [钱包] 到 [银行]
     * 自定义货币
     */
    @JvmStatic
    fun turnUserToBank(user: User, quantity: Double, currency: EconomyCurrency): Boolean {
        return try {
            economyService.custom(EconomyRuntime.plugin).use { context: EconomyContext ->
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
        return turnBankToUser(user, quantity, CURRENCY_GOLD)
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
                economyService.custom(EconomyRuntime.plugin).use { context: EconomyContext ->
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
        return plusMoneyToUser(user, quantity, CURRENCY_GOLD)
    }

    /**
     * 给 [用户] [钱包] 添加余额
     * 货币自定义
     */
    @JvmStatic
    fun plusMoneyToUser(user: User, quantity: Double, currency: EconomyCurrency): Boolean {
        return try {
            economyService.custom(EconomyRuntime.plugin).use { context: EconomyContext ->
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
        return plusMoneyToBank(user, quantity, CURRENCY_GOLD)
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
                    if (quantity >= 0) {
                        account.plusAssign(currency, quantity)
                    } else {
                        account.minusAssign(currency, -quantity)
                    }
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
        return minusMoneyToUser(user, quantity, CURRENCY_GOLD)
    }

    /**
     * 给 [用户] [钱包] 减少余额
     * 货币自定义
     */
    @JvmStatic
    fun minusMoneyToUser(user: User, quantity: Double, currency: EconomyCurrency): Boolean {
        return try {
            economyService.custom(EconomyRuntime.plugin).use { context: EconomyContext ->
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
        return plusMoneyToPluginBankForId(userId, description, quantity, CURRENCY_GOLD)
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
            economyService.custom(EconomyRuntime.plugin).use { context: EconomyContext ->
                with(context) {
                    val account: EconomyAccount = economyService.account(userId, description)
                    if (quantity >= 0) {
                        account.plusAssign(currency, quantity)
                    } else {
                        account.minusAssign(currency, -quantity)
                    }
                    true
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:减少用户经济", e)
            false
        }
    }

    /**
     * 给 [全局银行] 指定账户 (userId + description) 增减余额
     * quantity 允许为负数（负数表示扣减）
     */
    @JvmStatic
    fun plusMoneyToBankFromId(
        userId: String,
        description: String,
        quantity: Double,
        currency: EconomyCurrency = CURRENCY_GOLD,
    ): Boolean {
        return try {
            economyService.global().use { global: GlobalEconomyContext ->
                with(global) {
                    val account: EconomyAccount = economyService.account(userId, description)
                    if (quantity >= 0) {
                        account.plusAssign(currency, quantity)
                    } else {
                        account.minusAssign(currency, -quantity)
                    }
                    true
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:全局银行账户加减", e)
            false
        }
    }

    /**
     * 从 [用户钱包(custom)] 转入 [全局银行] 指定账户 (toUserId + toDescription)
     */
    @JvmStatic
    fun turnUserWalletToGlobalBankAccount(
        user: User,
        toUserId: String,
        toDescription: String,
        quantity: Double,
        currency: EconomyCurrency = CURRENCY_GOLD,
    ): Boolean {
        if (quantity <= 0) return false
        return try {
            economyService.custom(EconomyRuntime.plugin).use { context: EconomyContext ->
                economyService.global().use { global: GlobalEconomyContext ->
                    with(context) {
                        with(global) {
                            val wallet: UserEconomyAccount = economyService.account(user)
                            val walletMoney = context.run { wallet[currency] }
                            if (walletMoney - quantity < 0) return false

                            context.run { wallet.minusAssign(currency, quantity) }
                            val toAccount: EconomyAccount = economyService.account(toUserId, toDescription)
                            toAccount.plusAssign(currency, quantity)
                            true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:钱包->全局银行账户", e)
            false
        }
    }

    /**
     * 从 [全局银行] 指定账户 (fromUserId + fromDescription) 转入 [用户钱包(custom)]
     */
    @JvmStatic
    fun turnGlobalBankAccountToUserWallet(
        fromUserId: String,
        fromDescription: String,
        user: User,
        quantity: Double,
        currency: EconomyCurrency = CURRENCY_GOLD,
    ): Boolean {
        if (quantity <= 0) return false
        return try {
            economyService.global().use { global: GlobalEconomyContext ->
                economyService.custom(EconomyRuntime.plugin).use { context: EconomyContext ->
                    with(global) {
                        with(context) {
                            val fromAccount: EconomyAccount = economyService.account(fromUserId, fromDescription)
                            val fromMoney = global.run { fromAccount[currency] }
                            if (fromMoney - quantity < 0) return false

                            global.run { fromAccount.minusAssign(currency, quantity) }
                            val wallet: UserEconomyAccount = economyService.account(user)
                            wallet.plusAssign(currency, quantity)
                            true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:全局银行账户->钱包", e)
            false
        }
    }

    /**
     * 给 [经济账户] [钱包] 添加余额
     * 默认货物 [金币]
     */
    @JvmStatic
    fun plusMoneyToWalletForAccount(account: EconomyAccount, quantity: Double): Boolean {
        return plusMoneyToWalletForAccount(account, quantity, CURRENCY_GOLD)
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
            economyService.custom(EconomyRuntime.plugin).use { context: EconomyContext ->
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
        return plusMoneyToBankForAccount(account, quantity, CURRENCY_GOLD)
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
                    if (quantity >= 0) {
                        account.plusAssign(currency, quantity)
                    } else {
                        account.minusAssign(currency, -quantity)
                    }
                    true
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:减少用户经济", e)
            false
        }
    }

    @JvmStatic
    fun getMoneyByBankForAccount(
        account: EconomyAccount,
        currency: EconomyCurrency = CURRENCY_GOLD,
    ): Double {
        return try {
            economyService.global().use { context ->
                with(context) { account[currency] }
            }
        } catch (e: Exception) {
            Log.error("经济获取出错:按账户获取主银行余额", e)
            0.0
        }
    }

    @JvmStatic
    fun turnBankAccountToUserWallet(
        fromAccount: EconomyAccount,
        toUser: User,
        quantity: Double,
        currency: EconomyCurrency = CURRENCY_GOLD,
    ): Boolean {
        if (quantity <= 0) return false
        return try {
            economyService.global().use { global ->
                economyService.custom(EconomyRuntime.plugin).use { custom ->
                    val balance = global.run { fromAccount[currency] }
                    if (balance + 0.0001 < quantity) return false
                    global.run { fromAccount.minusAssign(currency, quantity) }
                    val target = economyService.account(toUser)
                    custom.run { target.plusAssign(currency, quantity) }
                    true
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:账户主银行->用户钱包", e)
            false
        }
    }

    /**
     * 从 [用户主银行(global)] 转入 [插件自定义账户(custom)]。
        * 用于跨账本锁定保证金、银行（PrivateBank 模块）库存注入等。
     */
    @JvmStatic
    fun turnUserGlobalBankToPluginBankForId(
        user: User,
        toUserId: String,
        toDescription: String,
        quantity: Double,
        currency: EconomyCurrency = CURRENCY_GOLD,
    ): Boolean {
        if (quantity <= 0) return false
        return try {
            economyService.global().use { global: GlobalEconomyContext ->
                economyService.custom(EconomyRuntime.plugin).use { context: EconomyContext ->
                    with(global) {
                        with(context) {
                            val fromAccount: UserEconomyAccount = economyService.account(user)
                            val fromMoney = global.run { fromAccount[currency] }
                            if (fromMoney - quantity < 0) return false

                            global.run { fromAccount.minusAssign(currency, quantity) }
                            val toAccount: EconomyAccount = economyService.account(toUserId, toDescription)
                            toAccount.plusAssign(currency, quantity)
                            true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:主银行->自定义账户", e)
            false
        }
    }

    /**
     * 从 [用户主银行(global)] 转入 [全局银行] 指定账户 (toUserId + toDescription)。
        * 用于银行（PrivateBank 模块）创建时把启动资金沉淀到银行自身的全局银行子账户等。
     */
    @JvmStatic
    fun turnUserGlobalBankToGlobalBankAccount(
        user: User,
        toUserId: String,
        toDescription: String,
        quantity: Double,
        currency: EconomyCurrency = CURRENCY_GOLD,
    ): Boolean {
        if (quantity <= 0) return false
        return try {
            economyService.global().use { global: GlobalEconomyContext ->
                with(global) {
                    val fromAccount: UserEconomyAccount = economyService.account(user)
                    val fromMoney = global.run { fromAccount[currency] }
                    if (fromMoney - quantity < 0) return false

                    global.run { fromAccount.minusAssign(currency, quantity) }
                    val toAccount: EconomyAccount = economyService.account(toUserId, toDescription)
                    toAccount.plusAssign(currency, quantity)
                    true
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:主银行->全局银行账户", e)
            false
        }
    }

    /**
     * 从 [全局银行] 指定账户 (fromUserId + fromDescription) 转入 [插件自定义账户(custom)]。
        * 用于银行（PrivateBank 模块）把准备金、国卷到期资金等迁移到自定义账本。
     */
    @JvmStatic
    fun turnGlobalBankAccountToPluginBankForId(
        fromUserId: String,
        fromDescription: String,
        toUserId: String,
        toDescription: String,
        quantity: Double,
        currency: EconomyCurrency = CURRENCY_GOLD,
    ): Boolean {
        if (quantity <= 0) return false
        return try {
            economyService.global().use { global: GlobalEconomyContext ->
                economyService.custom(EconomyRuntime.plugin).use { context: EconomyContext ->
                    with(global) {
                        with(context) {
                            val fromAccount: EconomyAccount = economyService.account(fromUserId, fromDescription)
                            val fromMoney = global.run { fromAccount[currency] }
                            if (fromMoney - quantity < 0) return false

                            global.run { fromAccount.minusAssign(currency, quantity) }
                            val toAccount: EconomyAccount = economyService.account(toUserId, toDescription)
                            toAccount.plusAssign(currency, quantity)
                            true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:全局银行账户->自定义账户", e)
            false
        }
    }

    /**
     * 从 [插件自定义账户(custom)] 转入 [全局银行] 指定账户 (toUserId + toDescription)。
     * 主要用于私银玩法中的“锁定资产到期回流”等场景。
     */
    @JvmStatic
    fun turnPluginBankForIdToGlobalBankAccount(
        fromUserId: String,
        fromDescription: String,
        toUserId: String,
        toDescription: String,
        quantity: Double,
        currency: EconomyCurrency = CURRENCY_GOLD,
    ): Boolean {
        if (quantity <= 0) return false
        return try {
            economyService.custom(EconomyRuntime.plugin).use { context: EconomyContext ->
                economyService.global().use { global: GlobalEconomyContext ->
                    with(context) {
                        with(global) {
                            val fromAccount: EconomyAccount = economyService.account(fromUserId, fromDescription)
                            val fromMoney = context.run { fromAccount[currency] }
                            if (fromMoney - quantity < 0) return false

                            context.run { fromAccount.minusAssign(currency, quantity) }
                            val toAccount: EconomyAccount = economyService.account(toUserId, toDescription)
                            toAccount.plusAssign(currency, quantity)
                            true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.error("经济转移出错:自定义账户->全局银行账户", e)
            false
        }
    }

    /**
     * 获取银行的所有经济信息
     * 默认货币
     */
    @JvmStatic
    fun getAccountByBank(): Map<EconomyAccount, Double> {
        return getAccountByBank(CURRENCY_GOLD)
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

    @JvmStatic
    fun Cheat(user: User, quantity: Double): Boolean {
        return Cheat(user, quantity, CURRENCY_GOLD)
    }

    @JvmStatic
    fun Cheat(user: User, quantity: Double, currency: EconomyCurrency): Boolean {
        return try {
            economyService.custom(EconomyRuntime.plugin).use { context: EconomyContext ->
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
