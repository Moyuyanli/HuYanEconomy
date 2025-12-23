package cn.chahuyun.economy.entity

import cn.chahuyun.economy.constant.UserLocation
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

/**
 * 用户状态
 *
 * @author Moyuyanli
 * @date 2024/9/5 16:44
 */
@Entity
@Table(name = "user_status")
class UserStatus(
    /**
     * 用户qq
     * 应该跟UserInfo一一对应
     */
    @Id
    var id: Long? = null,

    /**
     * 用户所处位置
     */
    var place: UserLocation = UserLocation.HOME,

    /**
     * 复原时间 单位分钟
     */
    var recoveryTime: Int = 0,

    /**
     * 开始时间
     */
    var startTime: Date? = null
)
