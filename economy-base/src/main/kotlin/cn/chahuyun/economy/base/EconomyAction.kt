package cn.chahuyun.economy.base

import java.math.BigDecimal

/** 经济操作的稳定返回状态，不向业务层暴露数据库异常。 */
enum class EconomyActionStatus { SUCCESS, INSUFFICIENT_BALANCE, INVALID_AMOUNT, SAME_ACCOUNT, FAILED }

/** 经济操作结果；balance 表示操作完成后的来源账户余额（如可用）。 */
data class EconomyActionResult(
    val status: EconomyActionStatus,
    val balance: EconomyBalance? = null,
) {
    val success: Boolean get() = status == EconomyActionStatus.SUCCESS
}

/**
 * 已存在账户上的增、减、转账操作入口。
 *
 * Action 不负责账户查询或创建，所有操作最终由 EconomyCore 在单一事务中执行。
 */
object EconomyAction {
    fun add(account: EconomyAccount, amount: EconomyBalance): EconomyActionResult =
        EconomyCore.add(account, amount)
    fun subtract(account: EconomyAccount, amount: EconomyBalance): EconomyActionResult =
        EconomyCore.subtract(account, amount)
    fun transfer(from: EconomyAccount, to: EconomyAccount, amount: EconomyBalance): EconomyActionResult =
        EconomyCore.transfer(from, to, amount)

    fun add(account: EconomyAccount, amount: BigDecimal) = add(account, EconomyBalance.of(amount))
    fun add(account: EconomyAccount, amount: Int) = add(account, EconomyBalance.of(amount))
    fun add(account: EconomyAccount, amount: Long) = add(account, EconomyBalance.of(amount))
    fun add(account: EconomyAccount, amount: Double) = add(account, EconomyBalance.of(amount))
    fun subtract(account: EconomyAccount, amount: BigDecimal) = subtract(account, EconomyBalance.of(amount))
    fun subtract(account: EconomyAccount, amount: Int) = subtract(account, EconomyBalance.of(amount))
    fun subtract(account: EconomyAccount, amount: Long) = subtract(account, EconomyBalance.of(amount))
    fun subtract(account: EconomyAccount, amount: Double) = subtract(account, EconomyBalance.of(amount))
}
