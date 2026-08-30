package cn.chahuyun.economy.entity.item

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "ItemEntity")
@Table(name = "hye_item", indexes = [Index(name = "uk_hye_item_code", columnList = "code", unique = true)])
class ItemEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "code", length = 64, nullable = false, unique = true) var code: String = "",
    @Column(name = "name", length = 64, nullable = false) var name: String = "",
    @Column(name = "is_stackable", nullable = false) var stackable: Boolean = true,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now()
)
