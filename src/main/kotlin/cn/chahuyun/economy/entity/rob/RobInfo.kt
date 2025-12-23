package cn.chahuyun.economy.entity.rob

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity(name = "RobInfo")
@Table(name = "RobInfo")
class RobInfo(
    /**
     * 用户QQ号
     */
    @Id
    var userId: Long? = null,

    /**
     * 今日时间
     */
    var nowTime: Date? = null,

    /**
     * 被抢劫次数
     */
    var beRobNumber: Int? = null,

    /**
     * 抢劫成功次数
     */
    var robSuccess: Int? = null,

    /**
     * 打人成功次数
     */
    var hitSuccess: Int? = null
)
