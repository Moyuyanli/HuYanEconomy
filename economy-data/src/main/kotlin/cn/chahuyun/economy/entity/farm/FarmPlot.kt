package cn.chahuyun.economy.entity.farm

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "FarmPlot")
@Table(
    name = "FarmPlot",
    indexes = [Index(name = "idx_farm_plot_qq", columnList = "qq")]
)
class FarmPlot : Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(nullable = false)
    var qq: Long = 0

    @Column(nullable = false)
    var plotNo: Int = 0

    @Column(nullable = false, length = 16)
    var status: String = "LOCKED"

    @Column(length = 64)
    var cropCode: String = ""

    @Column(nullable = false)
    var plantedAt: Long = 0

    @Column(nullable = false)
    var currentSeason: Int = 0

    @Column(nullable = false)
    var nextMatureAt: Long = 0

    @Column(nullable = false, columnDefinition = "integer default 0")
    var stolenSeason: Int = 0

    @Column(nullable = false, columnDefinition = "integer default 0")
    var stolenAmount: Int = 0
}
