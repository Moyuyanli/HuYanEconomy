package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 鍥藉€哄彂琛孌TO
 */
@Serializable
data class PrivateBankGovBondIssueDto(
    /** 璁板綍ID */
    var id: Int = 0,
    /** 鍛ㄦ爣璇嗭紙鍞竴锛?*/
    var weekKey: String = "",
    /** 鏀剁泭鍊嶇巼 */
    var rateMultiplier: Double = 2.0,
    /** 閿佸畾澶╂暟 */
    var lockDays: Int = 3,
    /** 鎬婚搴﹂檺鍒?*/
    var totalLimit: Double = 0.0,
    /** 鍓╀綑棰濆害 */
    var remaining: Double = 0.0,
    /** 鍒涘缓鏃堕棿 */
    var createdAt: Long = 0
)
