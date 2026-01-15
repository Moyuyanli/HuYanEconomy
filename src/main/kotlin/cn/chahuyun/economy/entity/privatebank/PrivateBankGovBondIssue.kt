package cn.chahuyun.economy.entity.privatebank

import jakarta.persistence.*
import java.util.*

/**
 * 国卷发行（每周一轮）
 */
@Entity(name = "PrivateBankGovBondIssue")
@Table(name = "PrivateBankGovBondIssue")
class PrivateBankGovBondIssue(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    /** 周标识，例如 2026-W03 */
    @Column(unique = true)
    var weekKey: String = "",

    /** 利率倍数（相对主银行利率） */
    var rateMultiplier: Double = 2.0,

    /** 锁仓天数 */
    var lockDays: Int = 3,

    /** 总额度 */
    var totalLimit: Double = 0.0,

    /** 剩余额度 */
    var remaining: Double = 0.0,

    var createdAt: Date = Date(),
)
