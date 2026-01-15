package cn.chahuyun.economy.entity.privatebank

import jakarta.persistence.*
import java.util.*

/**
 * 私人银行（Private Banking System）
 *
 * 说明：
 * - 真实资金托管分别落在：
 *   - 主银行(global)：bankCode + "pb-reserve"（80%准备金池）
 *   - 自定义账本(custom)：bankCode + "pb-liquidity"（20%流动金池）/"pb-guarantee"（10M保证金）等
 * - 本实体主要记录规则参数、风控状态、评分等元数据。
 */
@Entity(name = "PrivateBank")
@Table(name = "PrivateBank")
class PrivateBank(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    /** 银行唯一编码（建议 pb-...） */
    @Column(unique = true)
    var code: String = "",

    /** 银行名称 */
    var name: String = "",

    /** Slogan */
    var slogan: String? = null,

    /** 行长 QQ */
    var ownerQq: Long = 0,

    /** 是否仅允许 VIP 存入（逗号分隔 QQ 白名单） */
    var vipOnly: Boolean = false,

    /** VIP 白名单，逗号分隔 */
    @Lob
    var vipWhitelist: String? = null,

    /** 给储户利率（与主银行 BankInfo.interest 同量纲，日利率 = interest/1000） */
    var depositorInterest: Int = 5,

    /** 注册/创建时间 */
    var createdAt: Date = Date(),

    /** 失信状态截止时间（不为空且未过期则处于失信） */
    var defaulterUntil: Date? = null,

    /** 取款请求数 */
    var withdrawRequests: Int = 0,

    /** 取款失败数 */
    var withdrawFailures: Int = 0,

    /** 星级（1-5） */
    var star: Int = 1,

    /** 平均评分（1-5） */
    var avgReview: Double = 0.0,
) {
    fun isDefaulter(now: Date = Date()): Boolean = defaulterUntil?.after(now) == true
}
