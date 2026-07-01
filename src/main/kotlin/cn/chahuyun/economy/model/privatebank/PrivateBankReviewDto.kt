package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 绉佷汉閾惰璇勪环DTO
 */
@Serializable
data class PrivateBankReviewDto(
    /** 璁板綍ID */
    var id: Int = 0,
    /** 閾惰缂栫爜 */
    var bankCode: String = "",
    /** 璇勪环浜篞Q */
    var userQq: Long = 0,
    /** 璇勫垎锛?-5锛?*/
    var rating: Int = 5,
    /** 璇勪环鍐呭 */
    var content: String = "",
    /** 鍒涘缓鏃堕棿 */
    var createdAt: Long = 0
)
