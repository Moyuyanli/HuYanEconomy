package cn.chahuyun.economy.utils

import cn.chahuyun.economy.base.ledger.EconomyLedger
import cn.chahuyun.economy.base.ledger.LedgerBook
import cn.chahuyun.economy.base.ledger.LedgerEntry
import cn.chahuyun.economy.base.ledger.LedgerOwner
import cn.chahuyun.economy.ledger.EconomyLedgerProvider
import net.mamoe.mirai.contact.User
import java.math.BigDecimal

/**
 * 经济账户门面（兼容层）。
 *
 * 所有方法都委托给当前生效的 [EconomyLedger]（economy-base 或上游 mirai-economy），
 * 业务代码不需要知道底层是哪套账本。方法签名保持 2.0.1 之前的 `Double` 形态，
 * 以最小化调用点改动；新代码建议直接使用 `EconomyLedger` + `BigDecimal`。
 *
 * 账户标识统一为 Mirai 权限 ID 形式（`u<qq>`），与 `UserInfoDto.id` 一致。
 */
object EconomyUtil {

    private val ledger: EconomyLedger
        get() = EconomyLedgerProvider.current

    /** 初始化当前经济后端；由插件启动流程调用，重复调用安全。 */
    @JvmStatic
    fun init() {
        EconomyLedgerProvider.init()
    }

    // region 读取

    /** 用户钱包余额。 */
    @JvmStatic
    fun getMoneyByUser(user: User): Double = ledger.balance(wallet(user)).toDouble()

    /** 用户主银行余额。 */
    @JvmStatic
    fun getMoneyByBank(user: User): Double = ledger.balance(userBank(user)).toDouble()

    /** 全局银行指定账户余额。 */
    @JvmStatic
    fun getMoneyByBankFromId(userId: String, description: String): Double =
        ledger.balance(LedgerOwner.GlobalAccount(userId, description)).toDouble()

    /** 插件私有指定账户余额。 */
    @JvmStatic
    fun getMoneyFromPluginBankForId(userId: String, description: String): Double =
        ledger.balance(LedgerOwner.PluginAccount(userId, description)).toDouble()

    /** 按账户 ID 读取主银行余额（原上游 `EconomyAccount` 参数版本）。 */
    @JvmStatic
    fun getMoneyByBankForAccount(accountId: String): Double =
        ledger.balance(LedgerOwner.UserBank(accountId)).toDouble()

    /**
     * 全局银行账本的完整余额列表（替代原来的 `getAccountByBank()`）。
     *
     * 每项的 [LedgerEntry.accountId] 就是原上游账户的 `uuid`，可直接用于
     * `UserCoreManager.getUserInfoByAccountId` 之类的反查。
     */
    @JvmStatic
    fun bankEntries(): List<LedgerEntry> = ledger.entries(LedgerBook.USER_BANK)

    /** 银行总存款。旧实现有 3 小时缓存，这里直接聚合，语义一致。 */
    @JvmStatic
    fun getBankTotalCached(): Double = ledger.total(LedgerBook.USER_BANK).toDouble()

    /** 兼容旧签名：当前实现不再维护额外缓存，调用即视为已刷新。 */
    @JvmStatic
    fun refreshBankTotalCache() {
        // 无缓存需要刷新；保留方法避免调用点改动。
    }

    // endregion

    // region 用户之间 / 用户与银行

    /** 用户钱包 → 用户钱包。 */
    @JvmStatic
    fun turnUserToUser(user: User, toUser: User, quantity: Double): Boolean =
        transfer(wallet(user), wallet(toUser), quantity)

    /** 用户钱包 → 主银行。 */
    @JvmStatic
    fun turnUserToBank(user: User, quantity: Double): Boolean =
        transfer(wallet(user), userBank(user), quantity)

    /** 主银行 → 用户钱包。 */
    @JvmStatic
    fun turnBankToUser(user: User, quantity: Double): Boolean =
        transfer(userBank(user), wallet(user), quantity)

    // endregion

    // region 增减

    /** 给用户钱包增减（负数为扣减，沿用旧语义：允许负余额）。 */
    @JvmStatic
    fun plusMoneyToUser(user: User, quantity: Double): Boolean = adjust(wallet(user), quantity)

    /** 给用户主银行增减（负数为扣减）。 */
    @JvmStatic
    fun plusMoneyToBank(user: User, quantity: Double): Boolean = adjust(userBank(user), quantity)

    /** 扣减用户钱包，余额不足返回 false。 */
    @JvmStatic
    fun minusMoneyToUser(user: User, quantity: Double): Boolean =
        adjust(wallet(user), -quantity)

    /** 给插件私有账户增减。 */
    @JvmStatic
    fun plusMoneyToPluginBankForId(userId: String, description: String, quantity: Double): Boolean =
        adjust(LedgerOwner.PluginAccount(userId, description), quantity)

    /** 给全局银行指定账户增减。 */
    @JvmStatic
    fun plusMoneyToBankFromId(
        userId: String,
        description: String,
        quantity: Double,
    ): Boolean = adjust(LedgerOwner.GlobalAccount(userId, description), quantity)

    /** 按账户 ID 增减主银行余额（原上游 `EconomyAccount` 参数版本）。 */
    @JvmStatic
    fun plusMoneyToBankForAccount(accountId: String, quantity: Double): Boolean =
        adjust(LedgerOwner.UserBank(accountId), quantity)

    /** 按账户 ID 增减钱包余额（原上游 `EconomyAccount` 参数版本）。 */
    @JvmStatic
    fun plusMoneyToWalletForAccount(accountId: String, quantity: Double): Boolean =
        adjust(LedgerOwner.Wallet(accountId), quantity)

    /** 主银行 → 用户钱包（原上游 `EconomyAccount` 参数版本）。 */
    @JvmStatic
    fun turnBankAccountToUserWallet(accountId: String, toUser: User, quantity: Double): Boolean =
        transfer(LedgerOwner.UserBank(accountId), wallet(toUser), quantity)

    /** 直接给钱包加钱（调试/补偿用）。 */
    @JvmStatic
    fun Cheat(user: User, quantity: Double): Boolean =
        ledger.add(wallet(user), quantity.money()).success

    // endregion

    // region 跨账本划转

    /** 用户钱包 → 全局银行指定账户。 */
    @JvmStatic
    fun turnUserWalletToGlobalBankAccount(
        user: User,
        toUserId: String,
        toDescription: String,
        quantity: Double,
    ): Boolean = transfer(wallet(user), LedgerOwner.GlobalAccount(toUserId, toDescription), quantity)

    /** 全局银行指定账户 → 用户钱包。 */
    @JvmStatic
    fun turnGlobalBankAccountToUserWallet(
        fromUserId: String,
        fromDescription: String,
        user: User,
        quantity: Double,
    ): Boolean = transfer(LedgerOwner.GlobalAccount(fromUserId, fromDescription), wallet(user), quantity)

    /** 用户主银行 → 插件私有账户。 */
    @JvmStatic
    fun turnUserGlobalBankToPluginBankForId(
        user: User,
        toUserId: String,
        toDescription: String,
        quantity: Double,
    ): Boolean = transfer(userBank(user), LedgerOwner.PluginAccount(toUserId, toDescription), quantity)

    /** 用户主银行 → 全局银行指定账户。 */
    @JvmStatic
    fun turnUserGlobalBankToGlobalBankAccount(
        user: User,
        toUserId: String,
        toDescription: String,
        quantity: Double,
    ): Boolean = transfer(userBank(user), LedgerOwner.GlobalAccount(toUserId, toDescription), quantity)

    /** 全局银行指定账户 → 插件私有账户。 */
    @JvmStatic
    fun turnGlobalBankAccountToPluginBankForId(
        fromUserId: String,
        fromDescription: String,
        toUserId: String,
        toDescription: String,
        quantity: Double,
    ): Boolean = transfer(
        LedgerOwner.GlobalAccount(fromUserId, fromDescription),
        LedgerOwner.PluginAccount(toUserId, toDescription),
        quantity,
    )

    /** 插件私有账户 → 全局银行指定账户。 */
    @JvmStatic
    fun turnPluginBankForIdToGlobalBankAccount(
        fromUserId: String,
        fromDescription: String,
        toUserId: String,
        toDescription: String,
        quantity: Double,
    ): Boolean = transfer(
        LedgerOwner.PluginAccount(fromUserId, fromDescription),
        LedgerOwner.GlobalAccount(toUserId, toDescription),
        quantity,
    )

    // endregion

    private fun wallet(user: User): LedgerOwner = LedgerOwner.Wallet("u${user.id}", user.nick)

    private fun userBank(user: User): LedgerOwner = LedgerOwner.UserBank("u${user.id}", user.nick)

    private fun transfer(from: LedgerOwner, to: LedgerOwner, quantity: Double): Boolean =
        ledger.transfer(from, to, quantity.money()).success

    private fun adjust(owner: LedgerOwner, quantity: Double): Boolean =
        ledger.adjust(owner, quantity.money()).success

    private fun Double.money(): BigDecimal = BigDecimal.valueOf(this)
}

/**
 * Kotlin 友好扩展，保持 2.0.1 之前的调用形态（去掉已无意义的货币参数）。
 */
fun User.walletBalance(): Double = EconomyUtil.getMoneyByUser(this)

fun User.bankBalance(): Double = EconomyUtil.getMoneyByBank(this)

fun User.depositToBank(quantity: Double): Boolean = EconomyUtil.turnUserToBank(this, quantity)

fun User.withdrawFromBank(quantity: Double): Boolean = EconomyUtil.turnBankToUser(this, quantity)

fun User.transferTo(toUser: User, quantity: Double): Boolean = EconomyUtil.turnUserToUser(this, toUser, quantity)
