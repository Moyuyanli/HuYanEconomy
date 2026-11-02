package cn.chahuyun.economy.ledger

import cn.chahuyun.economy.base.ledger.*
import cn.chahuyun.economy.ledger.EconomyProxy.backend
import cn.chahuyun.economy.ledger.EconomyProxy.bankEntries
import cn.chahuyun.economy.ledger.EconomyProxy.bankTotal
import cn.chahuyun.economy.ledger.EconomyProxy.global
import cn.chahuyun.economy.ledger.EconomyProxy.money
import cn.chahuyun.economy.ledger.EconomyProxy.plugin
import cn.chahuyun.economy.ledger.EconomyProxy.useBackend
import cn.chahuyun.economy.ledger.EconomyProxy.userBank
import cn.chahuyun.economy.ledger.EconomyProxy.userBankById
import cn.chahuyun.economy.ledger.EconomyProxy.wallet
import cn.chahuyun.economy.ledger.EconomyProxy.walletById
import cn.chahuyun.economy.ledger.EconomyProxy.walletId
import net.mamoe.mirai.contact.User
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 经济操作统一代理（2.0.1）——**业务代码唯一的经济入口**。
 *
 * 业务代码只描述"哪个位置、加还是减、多少钱"，不关心账户标识编码、金额精度、
 * 底层是 `economy-base`（本地账本）还是上游 mirai-economy：
 *
 * ```kotlin
 * EconomyProxy.wallet(user).balance()                 // 读余额
 * EconomyProxy.wallet(user).debit(money)              // 扣款（余额不足失败）
 * EconomyProxy.wallet(user).credit(money)             // 入账
 * EconomyProxy.wallet(user).adjust(-money)            // 带符号增减（允许透支，仅债务场景）
 * EconomyProxy.wallet(user).transferTo(EconomyProxy.userBank(user), money)  // 转账
 * ```
 *
 * ## 后端：默认本地实现，可选上游实现
 * 后端由 [backend] 解析，优先级如下：
 * 1. [useBackend] 注入的后端（测试、迁移脚本、嵌入式场景）；
 * 2. [EconomyLedgerProvider] 按 `Config.yml` 选出的后端：`economyBackend=BASE`（默认）用
 *    `BaseLedger` 本地账本；`MIRAI_ECONOMY` 用上游 mirai-economy，探测或自检失败时按
 *    `economyBackendFallback` 决定降级为 BASE 还是启动失败。
 *
 * 注入只影响本进程内的解析结果，不改变 `Config.yml`；`useBackend(null)` 即恢复配置选择。
 *
 * ## 四类位置
 * | 位置 | 构造方法 | `economy-base` 分类 | mirai 后端账户 id |
 * |------|----------|--------------------|-------------------|
 * | 用户钱包 | [wallet] / [walletById] | `WALLET` | `u<qq>` |
 * | 用户主银行 | [userBank] / [userBankById] | `USER_BANK` | `u<qq>#BANK` |
 * | 插件私有账户 | [plugin] | `PLUGIN` | `<id>#PLUGIN` |
 * | 全局银行账户 | [global] | `GLOBAL` | `<id>#GLOBAL` |
 *
 * mirai 后端**只用 `custom(plugin)` 一个上下文**，scope 编码进账户 id 后缀
 * （编码规则见 `ledger/MiraiAccountId.kt`）；`global()` 上下文只服务于迁移读取
 * 2.0.1 之前的旧布局，运行时路径不再触碰。
 *
 * ## 语义边界
 * - [Account.debit] 走 `subtract`：余额不足直接失败，**不会写出负余额**。
 * - [Account.adjust] 走 `adjust`：负数为"强制扣款"，允许把余额扣成负数，
 *   只给债务转移 / 破产清算一类场景用；普通扣款必须用 [Account.debit]。
 * - [Account.credit] / [Account.adjust] 的金额入参统一经 [money] 两位 HALF_UP 舍入，
 *   `Double` 重载也走同一入口，调用点不必自己做精度处理。
 *
 * ## 精度与账户编码
 * [money] 与 [walletId] 是本项目唯一的两个转换入口：业务层不要手写 `"u$qq"`、
 * 也不要用 `BigDecimal.valueOf` 直接拼金额。
 *
 * ## 使用约束
 * - **句柄无状态**：[Account] 不缓存余额，`balance()` 每次都查库；需要"读一次用多次"时
 *   自己接住返回值，不要反复调用。
 * - **不做业务校验**：代理只管资金增减，不判断"这个人能不能付""这个账户是否冻结"，
 *   那些属于 feature 层的 Rules。
 * - **不组合原子性**：`a.debit(x); b.credit(x)` 不是一次原子操作，需要严格的跨账户语义时
 *   用 [Account.transferTo]（base 后端单事务）而不是两步调用。
 * - **不要在热路径上做聚合**：[bankEntries] / [bankTotal] 是全表读，只给对账与排行用。
 *
 * ```kotlin
 * // 反例：绕过代理自己拼编码与金额
 * val id = "u${user.id}"
 * val amount = BigDecimal.valueOf(qty / 100.0)
 *
 * // 正例
 * EconomyProxy.wallet(user).credit(EconomyProxy.money(qty))
 * ```
 */
object EconomyProxy {

    /** 金额精度：与 `economy_account.balance DECIMAL(19,2)` 对齐。 */
    const val SCALE: Int = 2

    /**
     * 注入的后端；`null` 表示按配置选择。
     *
     * `@Volatile` 是因为注入通常发生在测试的 `@BeforeClass` / 迁移脚本的线程里，
     * 而读取方可能是任意业务线程；不保证复合操作原子性（本字段只会整体赋值，够用）。
     */
    @Volatile
    private var injected: EconomyLedger? = null

    /**
     * 当前生效的后端。
     *
     * @throws IllegalStateException 既未注入、[EconomyLedgerProvider] 也未初始化
     */
    val backend: EconomyLedger
        get() = injected ?: EconomyLedgerProvider.current

    /** 当前后端标识，用于日志与排查。 */
    val backendId: String
        get() = injected?.backendId ?: EconomyLedgerProvider.backendId

    /**
     * 注入后端，仅用于测试、迁移脚本等进程内场景。
     *
     * @param ledger 传入 `null` 恢复为按 `Config.yml` 选择的后端
     */
    fun useBackend(ledger: EconomyLedger?) {
        injected = ledger
    }

    /** 用户钱包；[nickname] 只服务 mirai 后端（上游 `description` 展示字段）。 */
    fun wallet(user: User): Account = Account(LedgerOwner.Wallet(walletId(user.id), user.nick))

    /** 用户钱包（仅有 QQ 号时使用）。 */
    fun wallet(qq: Long, nickname: String? = null): Account =
        Account(LedgerOwner.Wallet(walletId(qq), nickname))

    /** 按账户 ID 直接定位钱包；仅迁移、修复等必须按 id 操作的场景使用。 */
    fun walletById(accountId: String): Account = Account(LedgerOwner.Wallet(accountId))

    /** 用户主银行。 */
    fun userBank(user: User): Account = Account(LedgerOwner.UserBank(walletId(user.id), user.nick))

    /** 用户主银行（仅有 QQ 号时使用）。 */
    fun userBank(qq: Long, nickname: String? = null): Account =
        Account(LedgerOwner.UserBank(walletId(qq), nickname))

    /** 按账户 ID 定位主银行；账户 ID 与钱包同构（`u<qq>`）。 */
    fun userBankById(accountId: String): Account = Account(LedgerOwner.UserBank(accountId))

    /** 插件私有账户（私人银行的准备金 / 流动金 / 库存等）。 */
    fun plugin(accountId: String, description: String = ""): Account =
        Account(LedgerOwner.PluginAccount(accountId, description))

    /** 全局银行子账户（银行资金池等）。 */
    fun global(accountId: String, description: String = ""): Account =
        Account(LedgerOwner.GlobalAccount(accountId, description))

    /** 主银行账本全量条目，用于富豪榜、迁移与对账。 */
    fun bankEntries(): List<LedgerEntry> = backend.entries(LedgerBook.USER_BANK)

    /** 主银行存款总额，用于对账与聚合校验。 */
    fun bankTotal(): BigDecimal = backend.total(LedgerBook.USER_BANK)

    /**
     * QQ → 账本账户标识，与 `UserInfoDto.id` 同构。
     *
     * 全项目唯一的账户编码入口：`u<qq>` 与上游 `EconomyAccount.uuid` 同名，迁移无需转换。
     */
    fun walletId(qq: Long): String = "u$qq"

    /**
     * `Double` → 账本金额。
     *
     * 用 [BigDecimal.valueOf]（走 `Double.toString`）而不是 `BigDecimal(double)`，
     * 后者会把 `100.0` 展开成 `100.0000000000000000000...`。随后两位 HALF_UP 舍入，
     * 与 `EconomyBalance.of` 的口径一致。
     */
    fun money(amount: Double): BigDecimal =
        BigDecimal.valueOf(amount).setScale(SCALE, RoundingMode.HALF_UP)

    /** 已是 `BigDecimal` 的金额同样对齐到两位小数。 */
    fun money(amount: BigDecimal): BigDecimal = amount.setScale(SCALE, RoundingMode.HALF_UP)

    /**
     * 一个账本位置的句柄。
     *
     * 句柄本身不持有 [EconomyLedger] 实例，每次操作都向 [backend] 取当前后端，
     * 避免启动期构造顺序问题、后端切换或注入后的悬挂引用。
     *
     * 构造器是 `internal`：外部只能经 [wallet] / [userBank] / [plugin] / [global] 拿到句柄，
     * 保证账户编码永远由本类统一生成，不允许调用方自己 `Account(LedgerOwner.Wallet("u$qq"))`。
     *
     * 句柄**无状态、可安全跨线程传递**：真正的并发安全由后端实现决定
     * （`BaseLedger` 走事务/悲观锁，mirai 依赖上游锁）。
     */
    class Account internal constructor(val owner: LedgerOwner) {

        /** 余额；账户不存在时由后端创建零余额账户后返回 0。 */
        fun balance(): BigDecimal = EconomyProxy.backend.balance(owner)

        /** 余额的 `Double` 视图，供仍在用 `Double` 的领域模型做比较与格式化。 */
        fun balanceDouble(): Double = balance().toDouble()

        /** 入账；[amount] 必须为正数。 */
        fun credit(amount: BigDecimal): LedgerOp = EconomyProxy.backend.add(owner, money(amount))

        /** 入账（`Double` 入口，两位 HALF_UP 舍入）。 */
        fun credit(amount: Double): LedgerOp = credit(money(amount))

        /** 扣款；余额不足返回失败，不会写出负余额。 */
        fun debit(amount: BigDecimal): LedgerOp = EconomyProxy.backend.subtract(owner, money(amount))

        /** 扣款（`Double` 入口）。 */
        fun debit(amount: Double): LedgerOp = debit(money(amount))

        /**
         * 带符号增减：正数入账、负数强制扣款（允许负余额）、0 直接成功。
         *
         * 负数分支沿用旧门面 `plusMoneyTo*(..., -x)` 的语义，**只允许债务转移类场景使用**。
         */
        fun adjust(amount: BigDecimal): LedgerOp = EconomyProxy.backend.adjust(owner, money(amount))

        /** 带符号增减（`Double` 入口）。 */
        fun adjust(amount: Double): LedgerOp = adjust(money(amount))

        /** 转账到另一位置；由后端保证（base）或补偿实现（mirai）原子性。 */
        fun transferTo(target: Account, amount: BigDecimal): LedgerOp =
            EconomyProxy.backend.transfer(owner, target.owner, money(amount))

        /** 转账（`Double` 入口）。 */
        fun transferTo(target: Account, amount: Double): LedgerOp = transferTo(target, money(amount))

        override fun toString(): String = "Account($owner)"
    }
}
