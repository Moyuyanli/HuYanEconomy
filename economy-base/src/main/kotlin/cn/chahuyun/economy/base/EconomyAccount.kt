package cn.chahuyun.economy.base

import cn.chahuyun.economy.base.entity.EconomyAccountEntity
import net.mamoe.mirai.contact.User
import java.math.BigDecimal

/**
 * 账户的脱离实体句柄。
 *
 * 账户创建和读取统一通过 [take] 完成，余额修改必须交给 EconomyAction；
 * 该类不保存 Hibernate Session，也不允许业务层直接修改实体。
 */
class EconomyAccount internal constructor(
    val id: Long,
    val account: String,
    val scope: EconomyScope,
    val description: String?,
    balance: BigDecimal,
) {
    var balance: EconomyBalance = EconomyBalance.of(balance)
        internal set

    /** 将当前余额格式化为展示文本。 */
    fun formatBalance(scale: Int = 2): String = balance.format(scale)

    companion object {
        const val DEFAULT_SCOPE = "DEFAULT"

        /**
         * 获取一个经济用户
         */
        fun take(account: String, scope: EconomyScope? = null): EconomyAccount =
            EconomyCore.take(account, scope ?: EconomyCore.scope(DEFAULT_SCOPE)
            ?: error("默认 Scope 未初始化"))

        /** 获取用户的默认个人账户。 */
        fun from(user: User): EconomyAccount = take("u${user.id}")

        /** 获取用户在指定群域中的个人账户。 */
        fun userInGroup(userId: Long, groupId: Long): EconomyAccount =
            take("u${userId}g${groupId}")

        /** 获取指定群的公共资金池账户。 */
        fun group(groupId: Long): EconomyAccount = take("g$groupId")

        fun scopeName(name: String): EconomyScope? = EconomyCore.scopeName(name)
        fun scope(code: String): EconomyScope? = EconomyCore.scope(code)
        fun scopeCreate(name: String, code: String): EconomyScope = EconomyCore.scopeCreate(name, code)
        fun scopeDelete(scope: EconomyScope): Boolean = EconomyCore.scopeDelete(scope)
    }

    internal fun toEntity(): EconomyAccountEntity = error("账户句柄不能直接转换为实体")
}
