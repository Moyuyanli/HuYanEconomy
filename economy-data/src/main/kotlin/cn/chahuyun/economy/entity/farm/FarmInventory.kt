package cn.chahuyun.economy.entity.farm

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "FarmInventory")
@Table(
    name = "FarmInventory",
    indexes = [Index(name = "idx_farm_inventory_qq", columnList = "qq")]
)
class FarmInventory : Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(nullable = false)
    var qq: Long = 0

    @Column(nullable = false, length = 16)
    var itemType: String = ""

    @Column(nullable = false, length = 64)
    var itemCode: String = ""

    @Column(nullable = false)
    var amount: Int = 0
}
