package cn.chahuyun.economy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

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
     * 总抽奖次数
     */
    private Integer times;

    /**
     * 总头奖次数
     */
    private Integer jackpot;

    /**
     * 抽奖池次数
     */
    @ElementCollection
    @CollectionTable(
            name = "user_raffle_pool_times",  // 单独的表来存储 map 数据
            joinColumns = @JoinColumn(name = "user_id")  // 外键指向 user_raffle.id
    )
    @MapKeyColumn(name = "pool_name")     // Map 的 key（String）
    @Column(name = "times")               // Map 的 value（Integer）
    private Map<String, Integer> poolTimes;

    public UserRaffle() {
    }

    public UserRaffle(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDefaultPool() {
        return defaultPool;
    }

    public void setDefaultPool(String defaultPool) {
        this.defaultPool = defaultPool;
    }

    public Integer getTimes() {
        return times;
    }

    public void setTimes(Integer times) {
        this.times = times;
    }

    public Integer getJackpot() {
        return jackpot;
    }

    public void setJackpot(Integer jackpot) {
        this.jackpot = jackpot;
    }

    public Map<String, Integer> getPoolTimes() {
        return poolTimes;
    }

    public void setPoolTimes(Map<String, Integer> poolTimes) {
        this.poolTimes = poolTimes;
    }
}
