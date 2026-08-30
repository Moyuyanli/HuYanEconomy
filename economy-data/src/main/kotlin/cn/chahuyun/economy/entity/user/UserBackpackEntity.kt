package cn.chahuyun.economy.entity.user

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "UserBackpackEntity")
@Table(name = "hye_user_backpack", indexes = [
    Index(name = "idx_hye_backpack_user_item", columnList = "user_id,item_id"),
    Index(name = "idx_hye_backpack_user_equipped", columnList = "user_id,is_equipped")
])
class UserBackpackEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "user_id", nullable = false) var userId: Long = 0,
    @Column(name = "item_id", nullable = false) var itemId: Long = 0,
    @Column(name = "item_code", length = 64, nullable = false) var itemCode: String = "",
    @Column(name = "item_name", length = 64, nullable = false) var itemName: String = "",
    @Column(name = "quantity", nullable = false) var quantity: Int = 1,
    @Column(name = "is_stackable", nullable = false) var stackable: Boolean = true,
    @Column(name = "is_equipped", nullable = false) var equipped: Boolean = false,
    @Lob @Column(name = "unique_attributes", columnDefinition = "JSON") var uniqueAttributes: String? = null,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now(),
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
) {
    fun touch(now: Instant = EntityTime.now()) { updatedAt = now }
}
