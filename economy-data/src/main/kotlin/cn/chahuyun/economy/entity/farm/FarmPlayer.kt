package cn.chahuyun.economy.entity.farm

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "FarmPlayer")
@Table(
    name = "FarmPlayer",
    indexes = [Index(name = "idx_farm_player_qq", columnList = "qq", unique = true)]
)
class FarmPlayer : Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(nullable = false)
    var qq: Long = 0

    @Column(nullable = false)
    var level: Int = 1

    @Column(nullable = false)
    var todayWaterCount: Int = 0

    @Column(length = 16)
    var lastWaterDate: String = ""

    @Column(nullable = false)
    var shieldUntil: Long = 0
}
