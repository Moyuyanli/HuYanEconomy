package cn.chahuyun.economy.entity.farm

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "UserFarmInventoryEntity")
@Table(name = "hye_user_farm_inventory", indexes = [
    Index(name = "uk_hye_farm_inventory_item", columnList = "user_id,item_type,target_id", unique = true),
    Index(name = "idx_hye_farm_inventory_type", columnList = "user_id,item_type")
])
class UserFarmInventoryEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "user_id", nullable = false) var userId: Long = 0,
    @Column(name = "item_type", length = 32, nullable = false) var itemType: String = "SEED",
    @Column(name = "target_id", nullable = false) var targetId: Long = 0,
    @Column(name = "target_code", length = 64, nullable = false) var targetCode: String = "",
    @Column(name = "target_name", length = 64, nullable = false) var targetName: String = "",
    @Column(name = "quantity", nullable = false) var quantity: Int = 1,
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)") var createdAt: Instant = EntityTime.now(),
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
)
