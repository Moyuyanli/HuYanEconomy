package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 绉佷汉閾惰璐锋浜у搧DTO
 */
@Serializable
data class PrivateBankLoanOfferDto(
    /** 璁板綍ID */
    var id: Int = 0,
    /** 閾惰缂栫爜 */
    var bankCode: String = "",
    /** 鎵€鏈夎€匭Q */
    var ownerQq: Long = 0,
    /** 璧勯噾鏉ユ簮 */
    var source: String = "LIQUIDITY",
    /** 鎬婚搴?*/
    var total: Double = 0.0,
    /** 鍓╀綑棰濆害 */
    var remaining: Double = 0.0,
    /** 鍒╃巼锛堢櫨鍒嗘瘮锛?*/
    var interest: Int = 10,
    /** 鍊熸澶╂暟 */
    var termDays: Int = 7,
    /** 鏄惁鍚敤 */
    var enabled: Boolean = true,
    /** 鍒涘缓鏃堕棿 */
    var createdAt: Long = 0
)
