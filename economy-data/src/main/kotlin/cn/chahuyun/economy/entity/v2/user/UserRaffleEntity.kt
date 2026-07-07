package cn.chahuyun.economy.entity.v2.user

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "UserRaffleEntityV2")
@Table(
    name = "hye_user_raffle",
    indexes = [Index(name = "idx_hye_user_raffle_user_id", columnList = "user_id", unique = true)]
)
class UserRaffleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "default_pool", length = 128)
    var defaultPool: String = "",

    @Column(name = "times", nullable = false)
    var times: Int = 0,

    @Column(name = "jackpot", nullable = false)
    var jackpot: Int = 0,

    @Lob
    @Column(name = "pool_times", columnDefinition = "TEXT")
    var poolTimes: String = "{}",

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0
) : Serializable
