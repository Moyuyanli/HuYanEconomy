package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 绉佷汉閾惰鍊熸璁板綍DTO
 */
@Serializable
data class PrivateBankLoanDto(
    /** 璁板綍ID */
    var id: Int = 0,
    /** 璐锋浜у搧ID */
    var offerId: Int = 0,
    /** 閾惰缂栫爜 */
    var bankCode: String = "",
    /** 璐锋柟QQ */
    var lenderQq: Long = 0,
    /** 鍊熸柟QQ */
    var borrowerQq: Long = 0,
    /** 鏈噾 */
    var principal: Double = 0.0,
    /** 搴旇繕鎬婚 */
    var dueTotal: Double = 0.0,
    /** 宸茶繕閲戦 */
    var repaidAmount: Double = 0.0,
    /** 鍒╃巼锛堢櫨鍒嗘瘮锛?*/
    var interest: Int = 10,
    /** 鍊熸澶╂暟 */
    var termDays: Int = 7,
    /** 鍒涘缓鏃堕棿 */
    var createdAt: Long = 0,
    /** 鍒版湡鏃堕棿 */
    var dueAt: Long = 0,
    /** 杩樻竻鏃堕棿 */
    var repaidAt: Long = 0
)
