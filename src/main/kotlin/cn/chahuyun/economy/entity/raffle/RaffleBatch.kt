package cn.chahuyun.economy.entity.raffle

import cn.chahuyun.economy.constant.RaffleType
import cn.chahuyun.economy.prizes.RaffleResult
import jakarta.persistence.*
import java.util.*

/**
 * 抽奖批次
 */
@Entity
@Table(name = "raffle_batch")
class RaffleBatch(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    /**
     * 用户id
     */
    @Column(name = "user_id")
    var userId: Long? = null,

    /**
     * 群id
     */
    @Column(name = "group_id")
    var groupId: Long? = null,

    /**
     * 抽奖池id
     */
    @Column(name = "pool_id")
    var poolId: String? = null,

    /**
     * 抽奖类型
     */
    @Column(name = "raffle_type")
    var raffleType: RaffleType? = null,

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    var createTime: Date? = null,

    /**
     * 抽奖明细
     */
    @OneToMany(mappedBy = "batch", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    var records: MutableList<RaffleRecord> = mutableListOf()
) {

    constructor(type: RaffleType, results: List<RaffleResult>) : this() {
        if (results.isEmpty()) {
            throw RuntimeException("构建抽奖记录出错，抽奖记录为空")
        }
        val result = results[0]
        this.raffleType = type
        this.groupId = result.groupId
        this.userId = result.userId
        this.poolId = result.pool.id
        this.records = results.map { RaffleRecord(it).apply { batch = this@RaffleBatch } }.toMutableList()
        this.createTime = Date()
    }
}
