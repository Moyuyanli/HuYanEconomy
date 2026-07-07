package cn.chahuyun.economy.entity

import jakarta.persistence.*

/**
 * 用户抽奖信息
 */
@Entity
@Table(name = "user_raffle")
class UserRaffle(
    /**
     * 用户qq
     * 应该跟UserInfo一一对应
     */
    @Id
    var id: Long? = null,

    /**
     * 默认抽奖池
     */
    @Column(name = "default_pool")
    var defaultPool: String? = null,

    /**
     * 总抽奖次数
     */
    var times: Int? = null,

    /**
     * 总头奖次数
     */
    var jackpot: Int? = null,

    /**
     * 抽奖池次数
     */
    @ElementCollection
    @CollectionTable(
        name = "user_raffle_pool_times",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @MapKeyColumn(name = "pool_name")
    @Column(name = "times")
    var poolTimes: MutableMap<String, Int> = mutableMapOf()
)
