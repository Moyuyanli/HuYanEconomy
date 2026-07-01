package cn.chahuyun.economy.entity.v2.raffle

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "RaffleBatchEntityV2")
@Table(
    name = "hye_raffle_batch",
    indexes = [
        Index(name = "idx_hye_raffle_batch_user_id", columnList = "user_id"),
        Index(name = "idx_hye_raffle_batch_pool_id", columnList = "pool_id")
    ]
)
class RaffleBatchEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "group_id", nullable = false)
    var groupId: Long = 0,

    @Column(name = "pool_id", length = 128)
    var poolId: String = "",

    @Column(name = "raffle_type", length = 64)
    var raffleType: String = "",

    @Column(name = "create_time", nullable = false)
    var createTime: Long = 0,

    @Column(name = "record_count", nullable = false)
    var recordCount: Int = 0,

    @Lob
    @Column(name = "records", columnDefinition = "TEXT")
    var records: String = "[]",

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0
) : Serializable
