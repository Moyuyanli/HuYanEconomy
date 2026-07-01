package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 鍥藉€烘寔浠揇TO
 */
@Serializable
data class PrivateBankGovBondHoldingDto(
    /** 璁板綍ID */
    var id: Int = 0,
    /** 閾惰缂栫爜 */
    var bankCode: String = "",
    /** 鍙戣ID */
    var issueId: Int = 0,
    /** 鏈噾 */
    var principal: Double = 0.0,
    /** 鏀剁泭鍊嶇巼 */
    var rateMultiplier: Double = 2.0,
    /** 閿佸畾澶╂暟 */
    var lockDays: Int = 3,
    /** 涔板叆鏃堕棿 */
    var boughtAt: Long = 0,
    /** 璧庡洖鏃堕棿 */
    var redeemedAt: Long = 0
)
