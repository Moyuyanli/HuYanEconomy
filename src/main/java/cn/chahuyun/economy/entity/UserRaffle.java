package cn.chahuyun.economy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户抽奖信息
 */
@Entity
@Table(name = "user_raffle")
@Getter
@Setter
public class UserRaffle {
    /**
     * 用户qq
     * 应该跟UserInfo一一对应
     */
    @Id
    private Long id;

    /**
     * 默认抽奖池
     */
    @Column(name = "default_pool")
    private String defaultPool;

    /**
     * 抽奖次数
     */
    private Integer times;

    /**
     * 头奖次数
     */
    private Integer jackpot;

}
