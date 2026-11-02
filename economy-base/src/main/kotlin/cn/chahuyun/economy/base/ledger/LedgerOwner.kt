package cn.chahuyun.economy.base.ledger

import java.math.BigDecimal

/**
 * 经济账本位置。
 *
 * 这是唯一需要被不同经济后端（economy-base 自建账户 / 上游 mirai-economy）共同理解的
 * 概念：业务代码只说"要操作哪个位置的资金"，至于落到哪张表、哪个上下文，由具体
 * [EconomyLedger] 实现决定。
 *
 * [accountId] 统一使用 Mirai 权限 ID 字符串（`u<qq>`、`g<群号>`），与
 * `UserInfoDto.id`、上游 mirai-economy 的 `EconomyAccount.uuid` 完全一致，
 * 因此两套后端可以共用同一份账户标识，迁移时无需做任何编码转换。
 */
sealed interface LedgerOwner {

    /**
     * 用户钱包（上游 custom 上下文中的用户账户）。
     *
     * [displayName] 只在 mirai-economy 后端使用：上游账户表的 `description` 是可变展示字段，
     * 传 null 会被上游覆盖成空串，因此能拿到昵称时应尽量传入。economy-base 后端忽略该值。
     */
    data class Wallet(val accountId: String, val displayName: String? = null) : LedgerOwner

    /** 用户主银行（上游 global 上下文中的用户账户），[displayName] 语义同 [Wallet]。 */
    data class UserBank(val accountId: String, val displayName: String? = null) : LedgerOwner

    /**
     * 插件私有资金账户，例如私人银行的准备金 / 流动金 / 库存池（上游 custom 上下文中的
     * 自定义账户）。
     *
     * [description] 仅用于展示和排查，不参与账户身份：上游的账户主键就是 uuid，
     * `description` 只是标签，两套后端都按 [accountId] 唯一。
     */
    data class PluginAccount(val accountId: String, val description: String = "") : LedgerOwner

    /**
     * 全局银行子账户（上游 global 上下文中的自定义账户），例如银行自身的资金池账户。
     * [description] 语义同 [PluginAccount]。
     */
    data class GlobalAccount(val accountId: String, val description: String = "") : LedgerOwner
}

/**
 * 账本分类，对应上游的 custom / global 上下文与账户类型。
 *
 * 这是[位置][LedgerOwner]的**扁平化视图**，只用于"按类聚合"的场景（迁移、排行、对账）；
 * 单笔读写一律用 [LedgerOwner]，因为分类无法表达具体是哪个账户。
 *
 * | 分类 | `BaseLedger` 的 Scope code | `MiraiEconomyLedger` 的 id 后缀 |
 * |------|---------------------------|--------------------------------|
 * | [WALLET] | `DEFAULT` | 无后缀（仅 `u<qq>` 形态） |
 * | [USER_BANK] | `BANK` | `#BANK` |
 * | [PLUGIN] | `PLUGIN` | `#PLUGIN` |
 * | [GLOBAL] | `GLOBAL` | `#GLOBAL` |
 */
enum class LedgerBook {
    /** 全部钱包账户（上游 custom 上下文全量）。 */
    WALLET,

    /** 全部主银行账户（上游 global 上下文全量）。 */
    USER_BANK,

    /** 插件私有自定义账户（上游 custom 上下文中的非用户账户）。 */
    PLUGIN,

    /** 全局银行自定义账户（上游 global 上下文中的非用户账户）。 */
    GLOBAL,
}

/**
 * 账本中的一条余额记录，用于迁移、总额统计与排行。
 *
 * @property accountId 账户标识，**不含** scope 后缀（`#BANK` 等已被实现剥掉）
 * @property description 展示名；`BaseLedger` 取库里的描述字段，mirai 实现因上游该字段
 *   可变、不可作为身份依据而返回 `null`
 * @property amount 余额，允许为负（债务场景）；迁移时按原值搬运，不取绝对值
 */
data class LedgerEntry(
    val accountId: String,
    val description: String?,
    val amount: BigDecimal,
)

/**
 * 单次经济操作的结果；不向业务层暴露数据库异常细节。
 *
 * @property success 是否成功。**唯一**可判断成败的字段，不要靠 `balance` 是否为空推断
 * @property balance 操作后的余额快照；失败时通常是操作前的余额（部分实现为 `null`），
 *   仅用于回显，不能当作账本真实状态
 * @property message 失败原因码，取值见 [EconomyLedger] 的"失败信息约定"表；
 *   成功时为 `null`
 */
data class LedgerOp(
    val success: Boolean,
    val balance: BigDecimal? = null,
    val message: String? = null,
) {
    companion object {
        /** 构造成功结果；可附带操作后余额。 */
        fun ok(balance: BigDecimal? = null) = LedgerOp(true, balance)

        /** 构造失败结果；[message] 建议使用错误码而非自由文本，便于调用方分支判断。 */
        fun fail(message: String? = null, balance: BigDecimal? = null) = LedgerOp(false, balance, message)
    }
}
