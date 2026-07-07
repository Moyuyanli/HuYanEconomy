package cn.chahuyun.economy.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 用户因子
 *
 * @author Moyuyanli
 * @date 2024/9/26 9:27
 */
@Entity(name = "UserFactor")
@Table
class UserFactor(
    @Id
    var id: Long? = null,

    /**
     * 暴躁值
     * 打他md
     */
    var irritable: Double = 0.3,

    /**
     * 武力值
     * 抢劫成功附加概率
     */
    @Column(name = "`force`")
    var force: Double = 0.1,

    /**
     * 闪避值
     * 各种地方的闪避、逃跑概率
     */
    var dodge: Double = 0.1,

    /**
     * 反抗因子
     * md,跟你爆了！
     */
    var resistance: Double = 0.3,

    /**
     * json存储格式
     */
    var buff: String? = "[]"
)
