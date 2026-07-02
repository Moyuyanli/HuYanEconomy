package cn.chahuyun.economy.entity.farm

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "FarmCrop")
@Table(
    name = "FarmCrop",
    indexes = [
        Index(name = "idx_farm_crop_code", columnList = "code", unique = true),
        Index(name = "idx_farm_crop_name", columnList = "name", unique = true)
    ]
)
class FarmCrop : Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(nullable = false, length = 64)
    var code: String = ""

    @Column(nullable = false)
    var level: Int = 0

    @Column(nullable = false)
    var upgradeCost: Long = 0

    @Column(nullable = false, length = 64)
    var name: String = ""

    @Column(nullable = false, length = 16)
    var emoji: String = ""

    @Column(nullable = false)
    var seedPrice: Int = 0

    @Column(nullable = false)
    var totalSeasons: Int = 1

    @Column(nullable = false)
    var yieldPerSeason: Int = 1

    @Column(nullable = false)
    var fruitPrice: Int = 0

    @Column(nullable = false)
    var firstMatureMinutes: Int = 0

    @Column(nullable = false)
    var nextMatureMinutes: Int = 0

    @Column(nullable = false)
    var totalRevenue: Int = 0

    @Column(nullable = false)
    var pureProfit: Int = 0

    @Column(nullable = false)
    var roi: Double = 0.0
}
