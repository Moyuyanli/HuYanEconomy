package cn.chahuyun.economy.entity.raffle;

import cn.chahuyun.economy.constant.RaffleType;
import cn.chahuyun.economy.prizes.RaffleResult;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * 抽奖批次
 */
@Entity
@Table(name = "raffle_batch")
@Getter
@Setter
public class RaffleBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户id
     */
    @Column(name = "user_id")
    private Long userId;
    /**
     * 群id
     */
    @Column(name = "group_id")
    private Long groupId;
    /**
     * 抽奖池id
     */
    @Column(name = "pool_id")
    private String poolId;
    /**
     * 抽奖类型
     */
    @Column(name = "raffle_type")
    private RaffleType raffleType;
    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private Date createTime;

    /*
     * 注意：JPA 关联中 mappedBy 是关键！
     *
     * ✅ 情况一：使用 mappedBy（双向关联）
     *   - 主表：@OneToMany(mappedBy = "batch")
     *   - 含义：关系由子表的 `batch` 属性维护
     *   - 子表必须有：@ManyToOne RaffleBatch batch;
     *   - 不能加 @JoinColumn 在主表上
     *   - 子表加 @JoinColumn(name = "batch_id") 由子表维护外键
     *
     * ✅ 情况二：不使用 mappedBy（单向关联）
     *   - 主表：@OneToMany + @JoinColumn(name = "batch_id")
     *   - 含义：主表直接控制外键，不依赖子表的实体引用
     *   - 子表只需有：@Column(name = "batch_id") Long batchId;
     *   - 不需要 @ManyToOne，也不需要 mappedBy
     *
     * ❌ 禁止写法：
     *   - 不能同时写 mappedBy 和 @JoinColumn（互斥！）
     *   - 不能写 mappedBy = "batch" 但子表只有 Long batchId（必须是实体引用）
     *
     * 💡 速记：
     *   "有 mappedBy，子表必有实体引用；
     *    无 mappedBy，主表自己控外键。"
     */

    /**
     * 抽奖明细
     */
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<RaffleRecord> records;

    public RaffleBatch() {
    }

    public RaffleBatch(RaffleType type, List<RaffleResult> results) {
        if (results.isEmpty()) {
            throw new RuntimeException("构建抽奖记录出错，抽奖记录为空");
        }
        var result = results.get(0);
        this.raffleType = type;
        this.userId = result.getGroupId();
        this.userId = result.getUserId();
        this.poolId = result.getPool().getId();
        this.records = results.stream().map(RaffleRecord::new).toList();
    }

}
