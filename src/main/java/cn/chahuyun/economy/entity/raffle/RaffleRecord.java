package cn.chahuyun.economy.entity.raffle;

import cn.chahuyun.economy.prizes.RaffleResult;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 抽奖明细
 */
@Entity
@Table(name = "raffle_record")
@Getter
@Setter
public class RaffleRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private RaffleBatch batch;

    /**
     * 奖品id
     */
    @Column(name = "prize_id")
    private String prizeId;

    /**
     * 奖品名称
     */
    @Column(name = "prize_name")
    private String prizeName;

    /**
     * 奖品等级
     */
    private Integer level;

    public RaffleRecord() {
    }

    public RaffleRecord(RaffleResult raffleResult) {
        this.prizeId = raffleResult.getPrize().getId();
        this.prizeName = raffleResult.getPrize().getName();
        this.level = raffleResult.getLevel();
    }

}
