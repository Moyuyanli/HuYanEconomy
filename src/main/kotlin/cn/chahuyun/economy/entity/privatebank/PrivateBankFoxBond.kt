package cn.chahuyun.economy.entity.privatebank

import jakarta.persistence.*
import java.util.*

/**
 * 狐卷（避免敏感词）
 *
 * - 发行日：每月 1 号与 15 号 08:00 起标，18:00 截标（由定时任务驱动）
 * - 竞标：行长提交溢价 + 接受利息
 * - 中标：扣除溢价并锁定面额资金，持有到期后自动回流
 */
@Entity(name = "PrivateBankFoxBond")
@Table(
    name = "PrivateBankFoxBond",
    indexes = [
        Index(name = "idx_pb_foxbond_status", columnList = "status"),
        Index(name = "idx_pb_foxbond_bidEndAt", columnList = "bidEndAt")
    ]
)
class PrivateBankFoxBond(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    /** 狐卷唯一 code */
    @Column(unique = true)
    var code: String = "",

    /** 面额 */
    var faceValue: Double = 0.0,

    /** 原始固定日利（百分比），例如 3.2 表示 3.2%/day */
    var baseRate: Double = 0.0,

    /** 期限（天） */
    var termDays: Int = 14,

    /** 竞标开始时间 */
    var bidStartAt: Date = Date(),

    /** 竞标截止时间 */
    var bidEndAt: Date = Date(),

    /** 状态：BIDDING / HOLDING / FINISHED / CANCELLED */
    var status: String = "BIDDING",

    /** 中标银行 code */
    var winnerBankCode: String? = null,

    /** 中标利率（日利，百分比） */
    var winnerBidRate: Double? = null,

    /** 中标溢价 */
    var winnerPremium: Double? = null,

    var createdAt: Date = Date(),
)
