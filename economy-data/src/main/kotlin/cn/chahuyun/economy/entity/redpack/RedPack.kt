package cn.chahuyun.economy.entity.redpack

import cn.hutool.core.util.StrUtil
import jakarta.persistence.*
import java.util.*

enum class RedPackType(val description: String) {
    NORMAL("普通红包"),
    RANDOM("随机红包"),
    PASSWORD("口令红包")
}

@Entity(name = "RedPack")
@Table(name = "RedPack")
class RedPack(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int? = null,

    /**
     * 红包名称
     */
    var name: String? = null,

    /**
     * 群id
     */
    var groupId: Long? = null,

    /**
     * 发送者
     */
    var sender: Long? = null,

    /**
     * 金币金额
     */
    var money: Double? = null,

    /**
     * 个数
     */
    var number: Int? = null,

    /**
     * 创建时间
     */
    var createTime: Date? = null,

    /**
     * 红包类型
     */
    @Enumerated(EnumType.STRING)
    var type: RedPackType = RedPackType.NORMAL,

    /**
     * 红包口令（仅口令红包使用）
     */
    var password: String? = null,

    /**
     * 已领走的钱数
     */
    var takenMoneys: Double = 0.0,

    /**
     * 领取者列表（存储为逗号分隔字符串）
     */
    var receivers: String? = null,

    /**
     * 随机红包金额列表（存储为逗号分隔字符串）
     */
    var randomRedPack: String? = null
) {

    /**
     * 领取红包的人
     */
    @Transient
    var receiverList: MutableList<Long> = mutableListOf()
        get() {
            if (field.isEmpty() && StrUtil.isNotBlank(receivers)) {
                receivers!!.split(",").filter { it.isNotBlank() }.forEach { field.add(it.toLong()) }
            }
            return field
        }
        set(value) {
            field = value
            receivers = value.joinToString(",")
        }

    /**
     * 随机红包列表
     */
    @Transient
    var randomPackList: MutableList<Double> = mutableListOf()
        get() {
            if (field.isEmpty() && StrUtil.isNotBlank(randomRedPack)) {
                randomRedPack!!.split(",").filter { it.isNotBlank() }.forEach { field.add(it.toDouble()) }
            }
            return field
        }
        set(value) {
            field = value
            randomRedPack = value.joinToString(",") { (Math.round(it * 10.0) / 10.0).toString() }
        }
}
