package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 鐙愮嫺鍊哄埜DTO
 */
@Serializable
data class PrivateBankFoxBondDto(
    /** 璁板綍ID */
    var id: Int = 0,
    /** 鍊哄埜缂栫爜锛堝敮涓€锛?*/
    var code: String = "",
    /** 闈㈠€?*/
    var faceValue: Double = 0.0,
    /** 鍩虹鍒╃巼 */
    var baseRate: Double = 0.0,
    /** 鏈熼檺澶╂暟 */
    var termDays: Int = 14,
    /** 绔炴爣寮€濮嬫椂闂?*/
    var bidStartAt: Long = 0,
    /** 绔炴爣缁撴潫鏃堕棿 */
    var bidEndAt: Long = 0,
    /** 鐘舵€侊紙BIDDING/COMPLETED/CANCELLED锛?*/
    var status: String = "BIDDING",
    /** 涓爣閾惰缂栫爜 */
    var winnerBankCode: String = "",
    /** 涓爣鍒╃巼 */
    var winnerBidRate: Double = 0.0,
    /** 涓爣婧环 */
    var winnerPremium: Double = 0.0,
    /** 鍒涘缓鏃堕棿 */
    var createdAt: Long = 0
)
