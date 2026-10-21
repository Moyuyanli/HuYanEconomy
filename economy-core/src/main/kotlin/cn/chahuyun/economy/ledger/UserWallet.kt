package cn.chahuyun.economy.ledger

import cn.chahuyun.economy.base.ledger.EconomyLedger
import cn.chahuyun.economy.base.ledger.LedgerOp
import cn.chahuyun.economy.base.ledger.LedgerOwner
import cn.chahuyun.economy.ledger.UserWallet.Companion.money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 用户钱包的业务级门面（**新经济接口**，`BigDecimal` 语义）。
 *
 * 业务代码只说"某个 QQ 的钱包里加/扣多少钱"，至于落到 `economy_account` 还是上游
 * mirai-economy 的自定义上下文，由构造时注入的 [EconomyLedger] 决定：
 *
 * - 生产环境用默认构造（[EconomyLedgerProvider.current]，按 `Config.yml` 选择后端）；
 * - 测试环境注入 [cn.chahuyun.economy.base.ledger.BaseLedger]，直接验证自建账本。
 *
 * ## 为什么需要这一层
 * 1. **账户标识只有一处**：`qq → "u<qq>"` 的映射集中在这里，业务层不再手写 `"u$qq"`。
 *    `u<qq>` 与 `UserInfoDto.id`、上游 `EconomyAccount.uuid` 完全同名，迁移无需转换。
 * 2. **金额精度只有一处**：签到奖励是 `Double`（随机档位 × 倍率），直接交给
 *    `DECIMAL(19,2)` 会带上二进制浮点误差；[money] 统一做两位 HALF_UP 舍入。
 * 3. **失败语义明确**：返回 [LedgerOp] 而不是 `Boolean`，能区分"余额不足""金额非法"
 *    "后端异常"，调用方可以给出不同提示。
 *
 * ## 与 `EconomyUtil` 的关系
 * `EconomyUtil` 是 2.0.1 之前的兼容门面（`Double` 入参、`Boolean` 返回）。新代码一律走
 * 本类；`EconomyUtil` 的调用点按 feature 逐步替换，替换完即可删掉兼容层。
 *
 * ## 注意
 * [balance] 在账户不存在时**会创建零余额账户**（`EconomyAccount.take` 的语义）。
 * 批量遍历 / 统计请用 `ledger.entries(book)`，不要逐条调 `balance`。
 *
 * @param ledger 实际使用的经济后端；默认取启动时选定的后端，未初始化时构造会抛错。
 */
class UserWallet(
    private val ledger: EconomyLedger = EconomyLedgerProvider.current,
) {

    /** 用户钱包余额；账户不存在时按后端语义创建零余额账户并返回 0。 */
    fun balance(qq: Long): BigDecimal = ledger.balance(owner(qq, null))

    /**
     * 给用户钱包入账。
     *
     * @param qq 用户 QQ。
     * @param amount 入账金额，必须为正数（已由 [money] 舍入到两位小数）。
     * @param nickname 展示用昵称，仅 mirai-economy 后端使用（上游 `description` 字段）；
     *   economy-base 后端忽略。
     * @return 操作结果，成功时 [LedgerOp.balance] 是**入账后**的余额，可直接用于回显，
     *   省掉一次额外查询。
     */
    fun credit(qq: Long, amount: BigDecimal, nickname: String? = null): LedgerOp =
        ledger.add(owner(qq, nickname), amount)

    /**
     * 从用户钱包扣款。
     *
     * 与 [credit] 相对；余额不足时返回失败（`INSUFFICIENT_BALANCE`），不会写出负余额。
     *
     * @param qq 用户 QQ。
     * @param amount 扣款金额，必须为正数。
     * @param nickname 展示用昵称，语义同 [credit]。
     */
    fun debit(qq: Long, amount: BigDecimal, nickname: String? = null): LedgerOp =
        ledger.subtract(owner(qq, nickname), amount)

    /** 当前后端标识，用于日志与排查。 */
    val backendId: String
        get() = ledger.backendId

    private fun owner(qq: Long, nickname: String?): LedgerOwner {
        require(qq > 0) { "用户 QQ 无效: $qq" }
        return LedgerOwner.Wallet(accountId(qq), nickname)
    }

    companion object {

        /** 金额精度：与 `economy_account.balance DECIMAL(19,2)` 对齐。 */
        const val SCALE: Int = 2

        /** 使用当前生效的经济后端。 */
        @JvmStatic
        fun current(): UserWallet = UserWallet()

        /** QQ → 账本账户标识，与 `UserInfoDto.id` 同构。 */
        @JvmStatic
        fun accountId(qq: Long): String = "u$qq"

        /**
         * `Double` → 账本金额。
         *
         * 用 [BigDecimal.valueOf]（走 `Double.toString`）而不是 `BigDecimal(double)`，
         * 后者会把 `100.0` 展开成 `100.0000000000000000000...`。随后两位 HALF_UP 舍入，
         * 与 `EconomyBalance.of` 的口径一致。
         */
        @JvmStatic
        fun money(amount: Double): BigDecimal =
            BigDecimal.valueOf(amount).setScale(SCALE, RoundingMode.HALF_UP)

        /** 已是 `BigDecimal` 的金额同样对齐到两位小数。 */
        @JvmStatic
        fun money(amount: BigDecimal): BigDecimal =
            amount.setScale(SCALE, RoundingMode.HALF_UP)
    }
}
