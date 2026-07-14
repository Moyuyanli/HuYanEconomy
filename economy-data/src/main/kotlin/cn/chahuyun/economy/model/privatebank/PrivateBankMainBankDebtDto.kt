package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/** 私人银行因强制兑付形成的主银行债务。 */
@Serializable
data class PrivateBankMainBankDebtDto(
    var id: Int = 0,
    var bankCode: String = "",
    var principal: Double = 0.0,
    var accruedInterest: Double = 0.0,
    var lastAccruedAt: Long = 0,
    var createdAt: Long = 0,
    var updatedAt: Long = 0,
    var repaidAt: Long = 0,
)
