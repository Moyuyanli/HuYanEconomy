package cn.chahuyun.economy.entity.raffle

import cn.chahuyun.economy.prizes.RaffleResult
import jakarta.persistence.*

/**
 * 抽奖明细
 */
@Entity
@Table(name = "raffle_record")
class RaffleRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    var batch: RaffleBatch? = null,

    /**
     * 奖品id
     */
    @Column(name = "prize_id")
    var prizeId: String? = null,

    /**
     * 奖品名称
     */
    @Column(name = "prize_name")
    var prizeName: String? = null,

    /**
     * 奖品等级
     */
    var level: Int? = null
) {

    constructor(raffleResult: RaffleResult) : this(
        prizeId = raffleResult.prize.id,
        prizeName = raffleResult.prize.name,
        level = raffleResult.level
    )
}
