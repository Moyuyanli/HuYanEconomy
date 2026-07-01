package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 鐙愮嫺鍊哄埜鎸佷粨DTO
 */
@Serializable
data class PrivateBankFoxBondHoldingDto(
    /** 璁板綍ID */
    var id: Int = 0,
    /** 鍊哄埜缂栫爜 */
    var bondCode: String = "",
    /** 閾惰缂栫爜 */
    var bankCode: String = "",
    /** 鏈噾 */
    var principal: Double = 0.0,
    /** 鍒╃巼 */
    var rate: Double = 0.0,
    /** 寮€濮嬫椂闂?*/
    var startedAt: Long = 0,
    /** 鍒版湡鏃堕棿 */
    var dueAt: Long = 0,
    /** 璧庡洖鏃堕棿 */
    var redeemedAt: Long = 0
)
