package cn.chahuyun.economy.entity.fish

import cn.chahuyun.economy.plugin.FishManager
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.hibernateplus.HibernateFactory
import jakarta.persistence.*
import java.io.Serializable
import java.util.regex.Pattern

/**
 * 鱼塘
 *
 * @author Moyuyanli
 * @date 2022/12/8 14:37
 */
@Entity(name = "FishPond")
@Table
class FishPond(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    /**
     * 鱼塘code
     * 群鱼塘 g[群号]
     * 私人鱼塘 [群号]-[玩家qq]
     * 私人全局鱼塘 [玩家qq]
     */
    var code: String = "",

    /**
     * 鱼塘管理者
     */
    var admin: Long = 0,

    /**
     * 鱼塘类型
     * 1-群鱼塘
     * 2-私人鱼塘
     * 3-全局鱼塘
     */
    var pondType: Int = 0,

    /**
     * 鱼塘名称
     */
    var name: String? = null,

    /**
     * 鱼塘描述
     */
    var description: String? = null,

    /**
     * 鱼塘等级
     */
    var pondLevel: Int = 0,

    /**
     * 限制最低进入等级
     */
    var minLevel: Int = 0,

    /**
     * 鱼塘钓的鱼出售回扣金额
     * 0.00-0.10
     */
    var rebate: Double = 0.05,

    /**
     * 总钓鱼次数
     */
    var number: Int = 0,

    @OneToMany(targetEntity = Fish::class, fetch = FetchType.EAGER)
    @JoinColumn(name = "pond_id")
    var fishList: MutableList<Fish>? = null
) : Serializable {

    constructor(pondType: Int, group: Long, admin: Long, name: String?, description: String?) : this(
        code = when (pondType) {
            1 -> "g-$group"
            2 -> "g-$group-$admin"
            else -> admin.toString()
        },
        admin = admin,
        name = name,
        description = description,
        pondLevel = if (pondType == 1) 6 else 1,
        pondType = pondType,
        minLevel = 0,
        rebate = 0.05,
        number = 0
    )

    val group: Long
        get() {
            val matcher = Pattern.compile("g-(\\d+)").matcher(code)
            return if (matcher.find()) matcher.group(1).toLong() else 0L
        }

    /**
     * 获取鱼塘的经济
     */
    fun getFishPondMoney(): Double {
        return EconomyUtil.getMoneyFromPluginBankForId(code, description ?: "")
    }

    /**
     * 获取池塘的鱼
     */
    fun getFishList(level: Int): List<Fish> {
        if (pondType == 1) {
            return FishManager.getLevelFishList(level)
        }
        return fishList ?: mutableListOf()
    }

    /**
     * 添加一次钓鱼次数
     */
    fun addNumber() {
        this.number++
        HibernateFactory.merge(this)
    }

    /**
     * 保存
     */
    fun save(): FishPond {
        return HibernateFactory.merge(this)!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FishPond) return false
        return code == other.code
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }
}
