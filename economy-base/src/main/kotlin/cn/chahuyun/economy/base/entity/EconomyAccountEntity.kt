package cn.chahuyun.economy.base.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

/** 账户余额持久化实体，仅允许 EconomyCore 在事务内访问。 */
@Entity
@Table(
    name = "economy_account",
    indexes = [
        Index(name = "uk_economy_account_scope", columnList = "account,scope_id", unique = true),
        Index(name = "idx_economy_account_scope", columnList = "scope_id")
    ]
)
class EconomyAccountEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "account", length = 64, nullable = false)
    var account: String = "",

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scope_id", nullable = false, foreignKey = ForeignKey(name = "fk_economy_account_scope"))
    var scope: EconomyScopeEntity? = null,

    @Column(name = "description", length = 255)
    var description: String? = null,

    @Column(name = "balance", precision = 19, scale = 2, nullable = false)
    var balance: BigDecimal = BigDecimal.ZERO.setScale(2),

    @Column(name = "update_time", nullable = false)
    var updateTime: Instant = Instant.now(),
)
