package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 绉佷汉閾惰瀛樻DTO
 */
@Serializable
data class PrivateBankDepositDto(
    /** 璁板綍ID */
    var id: Int = 0,
    /** 閾惰缂栫爜 */
    var bankCode: String = "",
    /** 瀛樻浜篞Q */
    var userQq: Long = 0,
    /** 鏈噾 */
    var principal: Double = 0.0,
    /** 瀛樺叆鏃堕棿 */
    var createdAt: Long = 0,
    /** 鏇存柊鏃堕棿 */
    var updatedAt: Long = 0
)
