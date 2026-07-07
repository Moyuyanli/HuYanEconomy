package cn.chahuyun.economy.entity.raffle

import cn.chahuyun.economy.constant.RaffleType
import cn.chahuyun.economy.model.raffle.RaffleRecordDto
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "raffle_batch")
class RaffleBatch(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id")
    var userId: Long? = null,

    @Column(name = "group_id")
    var groupId: Long? = null,

    @Column(name = "pool_id")
    var poolId: String? = null,

    @Column(name = "raffle_type")
    var raffleType: RaffleType? = null,

    @Column(name = "create_time")
    var createTime: Date? = null,

    @OneToMany(mappedBy = "batch", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    var records: MutableList<RaffleRecord> = mutableListOf()
) {

    constructor(
        type: RaffleType,
        userId: Long,
        groupId: Long,
        poolId: String,
        records: List<RaffleRecordDto>,
    ) : this() {
        require(records.isNotEmpty()) { "Raffle records must not be empty" }
        this.raffleType = type
        this.groupId = groupId
        this.userId = userId
        this.poolId = poolId
        this.records = records.map { RaffleRecord(it).apply { batch = this@RaffleBatch } }.toMutableList()
        this.createTime = Date()
    }
}
