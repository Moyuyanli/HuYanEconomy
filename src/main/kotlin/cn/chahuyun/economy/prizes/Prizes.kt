package cn.chahuyun.economy.prizes

import cn.chahuyun.economy.constant.PrizeType
import cn.chahuyun.economy.constant.RaffleType
import cn.chahuyun.economy.data.PrizesData
import cn.chahuyun.economy.entity.UserRaffle
import cn.chahuyun.economy.entity.raffle.RaffleBatch
import cn.chahuyun.economy.exception.RaffleException
import cn.chahuyun.hibernateplus.HibernateFactory
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Group
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random


/**
 * 奖品
 */
@Serializable
data class Prize(
    /**
     * 奖品id
     */
    val id: String,
    /**
     * 奖品名称
     */
    val name: String,
    /**
     * 奖品描述
     */
    val description: String,
    /**
     * 奖品图片
     */
    val imageUrl: String? = null,
    /**
     * 奖品类型
     */
    val type: PrizeType = PrizeType.ORDINARY,
    /**
     * 奖品元数据
     * 如: propsId=123, count=5
     */
    val metadata: Map<String, Int> = emptyMap(),
    /**
     * 奖品库存
     * -1 表示无限库存
     */
    var stock: Int = -1,
) {
    companion object {
        /**
         * 尝试根据id获取奖品
         */
        fun take(id: String): Prize {
            return PrizesData.prizes.find { it.id == id } ?: throw RaffleException("奖品不存在")
        }
    }

    // ✅ @Transient：不参与序列化，仅运行时存在
    @delegate:Transient
    val runtimeStock: AtomicInteger by lazy {
        AtomicInteger(stock)
    }

    /**
     * 原子性：尝试领取一次
     * @return true 成功，false 失败（无库存或无限库存也返回 true）
     */
    fun tryTake(): Boolean {
        val stock = runtimeStock // 无限库存永远成功
        while (true) {
            val current = stock.get()
            if (current <= 0) return false
            if (stock.compareAndSet(current, current - 1)) {
                this.stock -= 1
                return true
            }
            // CAS 失败，重试
        }
    }

    /**
     * 获取当前库存
     */
    @JvmName("getRuntimeStock")
    fun getStock(): Int {
        return runtimeStock.get()
    }

    /**
     * 补货（增加库存）
     */
    fun replenish(amount: Int) {
        val stock = runtimeStock
        stock.addAndGet(amount)
    }

}

/**
 * 奖品组
 */
@Serializable
data class PrizeGroup(
    /**
     * 奖品编号
     */
    val prizesCodes: List<String>,
    /**
     * 奖品组名称
     */
    val name: String = "default",
    /**
     * 奖品组描述
     */
    val description: String = "default",
    /**
     * 该组在所属层级中的权重
     */
    val weight: Int = 100,
    /**
     * 是否为“up”？
     */
    val guaranteed: Boolean = false,
) {
    /**
     * 奖品组内奖品
     */
    @delegate:Transient
    val prizes by lazy { prizesCodes.map { Prize.take(it) } }

    constructor(
        vararg prizes: String,
        name: String = "default",
        description: String = "default",
        weight: Int = 100,
        guaranteed: Boolean = false,
    ) : this(prizes.toList(), name, description, weight, guaranteed)

    fun getPrize(): Prize {
        return prizes.random()
    }
}

/**
 * 奖品等级
 */
@Serializable
data class PrizeLevel(
    /**
     * 奖品等级
     */
    val level: Int,
    /**
     * 该等级被抽中的权重（用于跨等级选择）
     */
    val weight: Int,
    /**
     * 所有组
     */
    val groups: List<PrizeGroup>,
) {

    constructor(level: Int, weight: Int, vararg groups: PrizeGroup) : this(level, weight, groups.toList())

    /**
     * up池
     */
    val guaranteedGroups by lazy { groups.filter { it.guaranteed } }

    /**
     * 普通池
     */
    val sharedGroups by lazy { groups.filter { !it.guaranteed } }

    /**
     * 是否 up 池
     */
    val isUpPrizePool by lazy { guaranteedGroups.isNotEmpty() }

    /**
     * up部分总权重
     */
    val guaranteedWeight by lazy { guaranteedGroups.sumOf { it.weight } }

    /**
     * 共享部分总权重
     */
    val sharedWeight by lazy { sharedGroups.sumOf { it.weight } }
}

/**
 * 奖池
 */
@Serializable
data class PrizePool(
    /**
     *
     */
    val id: String,
    /**
     * 奖池名称
     */
    val name: String,
    /**
     * 奖池描述
     */
    val description: String,
    /**
     * 抽奖价格
     */
    val price: Int,
    /**
     * 奖池物品
     */
    val levels: List<PrizeLevel>,
    /**
     * 保底次数
     */
    val endTime: Int = 0,

    /**
     * 是否共享保底
     */
    val shareEnd: Boolean = false,

    /**
     * 当前卡池抽奖次数
     */
    val shareEndTime: Int = 0,

    /**
     * 保底奖品
     */
    val endPrize: PrizeLevel? = null,
) {
    constructor(
        id: String,
        name: String,
        description: String,
        price: Int,
        vararg levels: PrizeLevel,
        endTime: Int = 0,
        shareEnd: Boolean = false,
        shareEndTime: Int = 0,
        endPrize: PrizeLevel? = null,
    ) : this(
        id,
        name,
        description,
        price,
        levels.toList(),
        endTime,
        shareEnd,
        shareEndTime,
        endPrize
    )
}

/**
 * 抽奖结果
 */
@Serializable
data class RaffleResult(
    val prize: Prize,
    val level: Int,
    val groupId: Long,
    val userId: Long,
    val pool: PrizePool,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * 抽奖上下文
 */
data class RaffleContext(
    /**
     * 奖池
     */
    val pool: PrizePool,
    /**
     * 用户抽奖信息
     */
    val userRaffle: UserRaffle,
    /**
     * 抽奖群
     */
    val group: Group,
)


/**
 * 对任意 List<T> 扩展一个 weightedRandom 方法
 * @param weightSelector: 函数，用于提取每个元素的权重
 * @param random: 随机数生成器（可选，便于测试）
 * @return 随机选中的元素，或 null（如果列表为空）
 */
private fun <T> List<T>.weightedRandom(
    random: Random = Random,
    weightSelector: (T) -> Int,
): T {
    if (isEmpty()) throw RaffleException("奖池错误,随机抽奖错误!")

    if (size == 1) return first()

    // 1. 计算总权重
    val totalWeight = sumOf { weightSelector(it).toLong() }

    // 2. 如果总权重 <= 0，退化为普通随机
    if (totalWeight <= 0) return this.random(random)

    // 3. 生成 [0, totalWeight) 的随机数
    var accumulatedWeight = random.nextLong(totalWeight)

    // 4. 遍历列表，减去权重，直到“命中”
    for (item in this) {
        //当前元素权重
        val weight = weightSelector(item).toLong()
        //命中数是否小于当前元素权重
        if (accumulatedWeight < weight) {
            return item
        }
        //减去权重
        accumulatedWeight -= weight
    }

    // 5. 理论上不会走到这里，但防止浮点/整数误差
    return last()
}

/**
 * 奖品工具类
 */
@OptIn(ConsoleExperimentalApi::class)
object PrizesUtil {
    /**
     * 单抽
     */
    fun PrizePool.draw(context: RaffleContext): RaffleResult {
        return drawing(context)
    }

    /**
     * 十连
     */
    fun PrizePool.drawTen(context: RaffleContext): List<RaffleResult> {
        val result = mutableListOf<RaffleResult>()
        for (i in 1..10) {
            result.add(drawing(context))
        }
        return result
    }

    /**
     * 抽奖操作
     */
    private fun drawing(context: RaffleContext): RaffleResult {
        val rafflePool = context.pool
        val userRaffle = context.userRaffle

        //检查保底
        if (rafflePool.endTime != 0) {

            val shareEndHit = rafflePool.shareEnd && rafflePool.shareEndTime + 1 == rafflePool.endTime
            val userEndHit = userRaffle.poolTimes[rafflePool.id]?.let { it + 1 == rafflePool.endTime } ?: false

            if (shareEndHit || userEndHit) {
                val level = rafflePool.endPrize ?: throw RaffleException("奖池配置保底，但奖池无保底奖品!")

                val groups = if (level.isUpPrizePool) {
                    level.guaranteedGroups
                } else {
                    level.sharedGroups
                }

                val group =
                    groups.weightedRandom { it.weight }

                val prize = group.getPrize()

                if (prize.getStock() != -1 && prize.tryTake()) {
                    throw RaffleException("限量奖没货了!")
                }

                val raffleResult = RaffleResult(prize, level.level, context.group.id, userRaffle.id, rafflePool)
                val raffleBatch = RaffleBatch(RaffleType.SINGLE, listOf(raffleResult))
                HibernateFactory.merge(raffleBatch)
                return raffleResult
            }
        }

        val level = rafflePool.levels.weightedRandom { it.weight }
        val group = level.groups.weightedRandom { it.weight }

        val prize = group.getPrize()

        val raffleResult = RaffleResult(prize, level.level, context.group.id, userRaffle.id, rafflePool)
        val raffleBatch = RaffleBatch(RaffleType.SINGLE, listOf(raffleResult))
        HibernateFactory.merge(raffleBatch)
        return raffleResult
    }

}