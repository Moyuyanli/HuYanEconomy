package cn.chahuyun.economy.image.model

/**
 * 银行信息图里的资金位置行。
 */
data class BankInfoFundLine(
    val label: String,
    val amount: String,
    val description: String = "",
)

/**
 * 银行信息图里的放贷/额度信息行。
 */
data class BankInfoLoanLine(
    val label: String,
    val value: String,
    val description: String = "",
)

/**
 * 银行信息图输入模型。
 */
data class PrivateBankInfoCard(
    val name: String,
    val code: String,
    val slogan: String,
    val owner: String,
    val star: Int,
    val interest: String,
    val avgReview: String,
    val totalDeposit: String,
    val withdrawSuccessRate: String,
    val defaulterUntil: String,
    val fundLines: List<BankInfoFundLine>,
    val loanLines: List<BankInfoLoanLine>,
)
