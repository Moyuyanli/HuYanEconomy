package cn.chahuyun.economy.model.bank

import kotlinx.serialization.Serializable

/**
 * 银行信息DTO
 */
@Serializable
data class BankInfoDto(
    /** 记录ID */
    val id: Int = 0,
    /** 银行编码 */
    val code: String = "",
    /** 银行名称 */
    val name: String = "",
    /** 银行描述 */
    val description: String = "",
    /** 管理员QQ */
    val qq: Long = 0,
    /** 是否开启利息 */
    val interestSwitch: Boolean = false,
    /** 注册时间 */
    val regTime: Long = 0,
    /** 注册时总金额 */
    val regTotal: Double = 0.0,
    /** 当前总金额 */
    val total: Double = 0.0,
    /** 利息率（百分比） */
    val interest: Int = 0
)
