package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 鐙愮嫺鍊哄埜绔炴爣DTO
 */
@Serializable
data class PrivateBankFoxBondBidDto(
    /** 璁板綍ID */
    var id: Int = 0,
    /** 鍊哄埜缂栫爜 */
    var bondCode: String = "",
    /** 閾惰缂栫爜 */
    var bankCode: String = "",
    /** 鎵€鏈夎€匭Q */
    var ownerQq: Long = 0,
    /** 婧环 */
    var premium: Double = 0.0,
    /** 绔炴爣鍒╃巼 */
    var bidRate: Double = 0.0,
    /** 鍒涘缓鏃堕棿 */
    var createdAt: Long = 0
)
