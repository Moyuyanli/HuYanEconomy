package cn.chahuyun.economy.entity.v2.redpack

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "RedPackEntityV2")
@Table(
    name = "hye_red_pack",
    indexes = [
        Index(name = "idx_hye_red_pack_group_id", columnList = "group_id"),
        Index(name = "idx_hye_red_pack_sender", columnList = "sender")
    ]
)
class RedPackEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "name", length = 128)
    var name: String = "",

    @Column(name = "group_id", nullable = false)
    var groupId: Long = 0,

    @Column(name = "sender", nullable = false)
    var sender: Long = 0,

    @Column(name = "money", nullable = false)
    var money: Double = 0.0,

    @Column(name = "number", nullable = false)
    var number: Int = 0,

    @Column(name = "create_time", nullable = false)
    var createTime: Long = 0,

    @Column(name = "type", length = 32, nullable = false)
    var type: String = "NORMAL",

    @Column(name = "password", length = 128)
    var password: String = "",

    @Column(name = "taken_moneys", nullable = false)
    var takenMoneys: Double = 0.0,

    @Lob
    @Column(name = "receivers", columnDefinition = "TEXT")
    var receivers: String = "",

    @Lob
    @Column(name = "random_red_pack", columnDefinition = "TEXT")
    var randomRedPack: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0
) : Serializable
