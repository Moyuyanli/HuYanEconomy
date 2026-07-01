package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 绉佷汉閾惰DTO
 */
@Serializable
data class PrivateBankDto(
    /** 璁板綍ID */
    var id: Int = 0,
    /** 閾惰缂栫爜锛堝敮涓€锛?*/
    var code: String = "",
    /** 閾惰鍚嶇О */
    var name: String = "",
    /** 閾惰鍙ｅ彿 */
    var slogan: String = "",
    /** 鎵€鏈夎€匭Q */
    var ownerQq: Long = 0,
    /** 鏄惁浠匳IP鍙瓨 */
    var vipOnly: Boolean = false,
    /** VIP鐧藉悕鍗曪紙JSON鏁扮粍锛?*/
    var vipWhitelist: String = "",
    /** 瀛樻鍒╃巼锛堢櫨鍒嗘瘮锛?*/
    var depositorInterest: Int = 5,
    /** 鍒涘缓鏃堕棿 */
    var createdAt: Long = 0,
    /** 澶变俊瑙ｉ櫎鏃堕棿 */
    var defaulterUntil: Long = 0,
    /** 鎻愮幇鐢宠娆℃暟 */
    var withdrawRequests: Int = 0,
    /** 鎻愮幇澶辫触娆℃暟 */
    var withdrawFailures: Int = 0,
    /** 璇勫垎鏄熺骇 */
    var star: Int = 1,
    /** 骞冲潎璇勫垎 */
    var avgReview: Double = 0.0
) {
    fun isDefaulter(now: Long = System.currentTimeMillis()): Boolean = defaulterUntil != 0L && defaulterUntil > now
}
