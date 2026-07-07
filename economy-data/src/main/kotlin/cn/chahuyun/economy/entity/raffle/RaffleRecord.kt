package cn.chahuyun.economy.entity.raffle

import cn.chahuyun.economy.model.raffle.RaffleRecordDto
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

    constructor(record: RaffleRecordDto) : this(
        id = record.id.takeIf { it != 0L },
        prizeId = record.prizeId,
        prizeName = record.prizeName,
        level = record.level
    )
}
