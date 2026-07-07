package cn.chahuyun.economy.entity.v2.rob

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "RobInfoEntityV2")
@Table(
    name = "hye_rob_info",
    indexes = [Index(name = "idx_hye_rob_info_user_id", columnList = "user_id", unique = true)]
)
class RobInfoEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "now_time", nullable = false)
    var nowTime: Long = 0,

    @Column(name = "be_rob_number", nullable = false)
    var beRobNumber: Int = 0,

    @Column(name = "rob_success", nullable = false)
    var robSuccess: Int = 0,

    @Column(name = "hit_success", nullable = false)
    var hitSuccess: Int = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0
) : Serializable
