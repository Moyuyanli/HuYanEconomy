package cn.chahuyun.economy.base.entity

import jakarta.persistence.*
import java.time.Instant

/** 经济域持久化实体；业务层通过 EconomyScope 脱离实体访问。 */
@Entity
@Table(
    name = "economy_scope",
    indexes = [Index(name = "uk_economy_scope_code", columnList = "code", unique = true)]
)
class EconomyScopeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "name", length = 64, nullable = false)
    var name: String = "",

    @Column(name = "code", length = 16, nullable = false, unique = true)
    var code: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
