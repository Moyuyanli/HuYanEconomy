package cn.chahuyun.economy.entity.redpack

import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.util.RandomUtil
import cn.hutool.core.util.StrUtil
import jakarta.persistence.*
import java.util.*
import java.util.stream.Collectors

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
     * 金额
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
     * 是否随机
     */
    @Column(name = "is_random_pack")
    var isRandomPack: Boolean = false,

    /**
     * 已领走的钱数
     */
    var takenMoneys: Double = 0.0,

    /**
     * 领取着
     */
    var receivers: String? = null,

    /**
     * 随机红包
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
                receivers!!.split(",").forEach { field.add(it.toLong()) }
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
                randomRedPack!!.split(",").forEach { field.add(it.toDouble()) }
            }
            return field
        }
        set(value) {
            field = value
            randomRedPack = value.joinToString(",") { (Math.round(it * 10.0) / 10.0).toString() }
        }

    /**
     * 获取随机红包
     *
     * @return 红包金额
     */
    fun getRandomPack(): Double {
        if (randomRedPack.isNullOrBlank() || randomPackList.isEmpty()) {
            throw RuntimeException("红包已经被领干净了，但仍然在领取!")
        }
        val index = RandomUtil.randomInt(0, randomPackList.size)
        val v = randomPackList[index]
        randomPackList.removeAt(index)
        this.randomPackList = randomPackList // Trigger setter to update randomRedPack string
        HibernateFactory.merge(this)
        return v
    }

    override fun toString(): String {
        return "RedPack(id=$id, name='$name', groupId=$groupId, sender=$sender, money=$money, number=$number, createTime=$createTime, isRandomPack=$isRandomPack, takenMoneys=$takenMoneys, receivers='$receivers', randomRedPack='$randomRedPack')"
    }
}
