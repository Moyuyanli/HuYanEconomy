package cn.chahuyun.economy.entity.v2

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "LotteryInfoEntityV2")
@Table(
    name = "hye_lottery_info",
    indexes = [
        Index(name = "idx_hye_lottery_type", columnList = "type"),
        Index(name = "idx_hye_lottery_qq", columnList = "qq")
    ]
)
class LotteryInfoEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "qq", nullable = false)
    var qq: Long = 0,

    @Column(name = "group_number", nullable = false)
    var group: Long = 0,

    @Column(name = "money", nullable = false)
    var money: Double = 0.0,

    @Column(name = "type", nullable = false)
    var type: Int = 0,

    @Column(name = "number", length = 128)
    var number: String = "",

    @Column(name = "current", length = 128)
    var current: String = "",

    @Column(name = "bonus", nullable = false)
    var bonus: Double = 0.0,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0
) : Serializable
