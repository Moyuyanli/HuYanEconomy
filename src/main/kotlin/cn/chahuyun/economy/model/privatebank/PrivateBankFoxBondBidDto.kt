package cn.chahuyun.economy.model.privatebank

import kotlinx.serialization.Serializable

/**
 * 狐狸债券竞标DTO
 */
@Serializable
data class PrivateBankFoxBondBidDto(
    /** 记录ID */
    var id: Int = 0,
    /** 债券编码 */
    var bondCode: String = "",
    /** 银行编码 */
    var bankCode: String = "",
    /** 所有者QQ */
    var ownerQq: Long = 0,
    /** 溢价 */
    var premium: Double = 0.0,
    /** 竞标利率 */
    var bidRate: Double = 0.0,
    /** 创建时间 */
    var createdAt: Long = 0
)
