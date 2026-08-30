package cn.chahuyun.economy.entity.user

import cn.chahuyun.economy.entity.common.EntityTime
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "UserEquipmentEntity")
@Table(name = "hye_user_equipment", indexes = [Index(name = "uk_hye_equipment_user", columnList = "user_id", unique = true)])
class UserEquipmentEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "user_id", nullable = false, unique = true) var userId: Long = 0,
    @Column(name = "title_id") var titleId: Long? = null,
    @Column(name = "hat_backpack_id") var hatBackpackId: Long? = null,
    @Column(name = "clothes_backpack_id") var clothesBackpackId: Long? = null,
    @Column(name = "pants_backpack_id") var pantsBackpackId: Long? = null,
    @Column(name = "shoes_backpack_id") var shoesBackpackId: Long? = null,
    @Column(name = "gloves_backpack_id") var glovesBackpackId: Long? = null,
    @Column(name = "left_hand_backpack_id") var leftHandBackpackId: Long? = null,
    @Column(name = "right_hand_backpack_id") var rightHandBackpackId: Long? = null,
    @Column(name = "fishing_rod_backpack_id") var fishingRodBackpackId: Long? = null,
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)") var updatedAt: Instant = EntityTime.now()
) {
    fun touch(now: Instant = EntityTime.now()) { updatedAt = now }
}
