package cn.chahuyun.economy.ledger

import cn.chahuyun.economy.base.ledger.LedgerBook
import cn.chahuyun.economy.base.ledger.LedgerOwner

/**
 * mirai-economy 后端的账户 id 编码规则。
 *
 * 上游账户坐标是 `(uuid, context)`，context 只有固定的几个值（`global-economy`
 * 与各插件的 `plugin.id`），无法承载 economy-base 可自由新增的 scope。
 * 因此 [MiraiEconomyLedger] 只用 `custom(plugin)` 一个上下文，把 scope 编码进 id：
 *
 * | [LedgerOwner] | 编码结果 |
 * | --- | --- |
 * | `Wallet("u123456")` | `u123456` |
 * | `UserBank("u123456")` | `u123456#BANK` |
 * | `PluginAccount("prv_001")` | `prv_001#PLUGIN` |
 * | `GlobalAccount("bank_pool")` | `bank_pool#GLOBAL` |
 *
 * 三条设计约定：
 * 1. **`Wallet` 不带后缀**。上游存量钱包就在 `(custom, u<qq>)`，同名可零搬迁。
 * 2. **分隔符用 `#`**。QQ 号与既有插件账户名都不含该字符。
 * 3. **解码取最后一个 `#` 并校验后缀白名单**。账户名自身含 `#` 时不会被截错，
 *    未知后缀（例如别人手写的 `foo#bar`）整体视为普通账户名。
 *
 * 附带收益：`u999#PLUGIN` 不再匹配 `^u\d+$`，即使误把用户形态的字符串当插件账户名，
 * 也不会在枚举时被当成用户钱包。
 */
internal object MiraiAccountId {

    const val SEPARATOR = '#'
    const val BANK = "BANK"
    const val PLUGIN = "PLUGIN"
    const val GLOBAL = "GLOBAL"

    /** 上游 `economy_account_record.uuid` 无显式长度限制，这里取保守上限。 */
    private const val MAX_LENGTH = 128

    private val SUFFIXES = setOf(BANK, PLUGIN, GLOBAL)

    private val USER_ACCOUNT = Regex("^u\\d+$")

    /** 解码结果：[accountId] 是原始账户标识，[suffix] 为 `null` 表示无 scope 后缀。 */
    data class Decoded(val accountId: String, val suffix: String? = null)

    /** 把账本位置编码成上游账户 id。 */
    fun encode(owner: LedgerOwner): String {
        val id = rawId(owner)
        val suffix = when (owner) {
            is LedgerOwner.Wallet -> return id
            is LedgerOwner.UserBank -> BANK
            is LedgerOwner.PluginAccount -> PLUGIN
            is LedgerOwner.GlobalAccount -> GLOBAL
        }
        val encoded = "$id$SEPARATOR$suffix"
        require(encoded.length <= MAX_LENGTH) { "经济账户标识过长(${encoded.length}): $encoded" }
        return encoded
    }

    /** 从上游账户 id 还原 `(accountId, suffix)`。 */
    fun decode(rawId: String): Decoded {
        val index = rawId.lastIndexOf(SEPARATOR)
        if (index <= 0 || index == rawId.lastIndex) return Decoded(rawId)
        val suffix = rawId.substring(index + 1)
        return if (suffix in SUFFIXES) {
            Decoded(rawId.substring(0, index), suffix)
        } else {
            Decoded(rawId)
        }
    }

    /** 判断一条上游余额记录是否属于 [book] 分类。 */
    fun belongsTo(decoded: Decoded, book: LedgerBook): Boolean = when (book) {
        // 钱包必须同时满足"无后缀"与"u<qq>"，避免把历史裸自定义账户算进钱包。
        LedgerBook.WALLET -> decoded.suffix == null && isUserAccount(decoded.accountId)
        LedgerBook.USER_BANK -> decoded.suffix == BANK
        LedgerBook.PLUGIN -> decoded.suffix == PLUGIN
        LedgerBook.GLOBAL -> decoded.suffix == GLOBAL
    }

    /** 是否为 `u<qq>` 形态的用户账户。 */
    fun isUserAccount(accountId: String): Boolean = USER_ACCOUNT.matches(accountId)

    /** 取出账本位置对应的原始账户标识（不带 scope 后缀）。 */
    fun rawId(owner: LedgerOwner): String {
        val id = when (owner) {
            is LedgerOwner.Wallet -> owner.accountId
            is LedgerOwner.UserBank -> owner.accountId
            is LedgerOwner.PluginAccount -> owner.accountId
            is LedgerOwner.GlobalAccount -> owner.accountId
        }.trim()
        require(id.isNotEmpty()) { "经济账户标识无效" }
        return id
    }
}
