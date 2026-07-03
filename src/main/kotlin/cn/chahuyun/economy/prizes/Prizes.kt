package cn.chahuyun.economy.prizes

import cn.chahuyun.economy.constant.PrizeType
import cn.chahuyun.economy.constant.RaffleType
import cn.chahuyun.economy.data.PrizesData
import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.exception.RaffleException
import cn.chahuyun.economy.model.raffle.RaffleBatchDto
import cn.chahuyun.economy.model.user.UserRaffleDto
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Group
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random


/**
 * 濂栧搧
 */
@Serializable
data class Prize(
    /**
     * 濂栧搧id
     */
    val id: String,
    /**
     * 濂栧搧鍚嶇О
     */
    val name: String,
    /**
     * 濂栧搧鎻忚堪
     */
    val description: String,
    /**
     * 濂栧搧鍥剧墖
     */
    val imageUrl: String? = null,
    /**
     * 濂栧搧绫诲瀷
     */
    val type: PrizeType = PrizeType.ORDINARY,
    /**
     * 濂栧搧鍏冩暟鎹?
     * 濡? propsId=123, count=5
     */
    val metadata: Map<String, Int> = emptyMap(),
    /**
     * 濂栧搧搴撳瓨
     * -1 琛ㄧず鏃犻檺搴撳瓨
     */
    var stock: Int = -1,
) {
    companion object {
        /**
         * 灏濊瘯鏍规嵁id鑾峰彇濂栧搧
         */
        fun take(id: String): Prize {
            return PrizesData.prizes.find { it.id == id } ?: throw RaffleException("奖品不存在")
        }
    }

    // 鉁?@Transient锛氫笉鍙備笌搴忓垪鍖栵紝浠呰繍琛屾椂瀛樺湪
    @delegate:Transient
    val runtimeStock: AtomicInteger by lazy {
        AtomicInteger(stock)
    }

    /**
     * 鍘熷瓙鎬э細灏濊瘯棰嗗彇涓€娆?
     * @return true 鎴愬姛锛宖alse 澶辫触锛堟棤搴撳瓨鎴栨棤闄愬簱瀛樹篃杩斿洖 true锛?
     */
    fun tryTake(): Boolean {
        val stock = runtimeStock // 鏃犻檺搴撳瓨姘歌繙鎴愬姛
        while (true) {
            val current = stock.get()
            if (current <= 0) return false
            if (stock.compareAndSet(current, current - 1)) {
                this.stock -= 1
                return true
            }
            // CAS 澶辫触锛岄噸璇?
        }
    }

    /**
     * 鑾峰彇褰撳墠搴撳瓨
     */
    @JvmName("getRuntimeStock")
    fun getStock(): Int {
        return runtimeStock.get()
    }

    /**
     * 琛ヨ揣锛堝鍔犲簱瀛橈級
     */
    fun replenish(amount: Int) {
        val stock = runtimeStock
        stock.addAndGet(amount)
    }

}

/**
 * 濂栧搧缁?
 */
@Serializable
data class PrizeGroup(
    /**
     * 濂栧搧缂栧彿
     */
    val prizesCodes: List<String>,
    /**
     * 濂栧搧缁勫悕绉?
     */
    val name: String = "default",
    /**
     * 濂栧搧缁勬弿杩?
     */
    val description: String = "default",
    /**
     * 璇ョ粍鍦ㄦ墍灞炲眰绾т腑鐨勬潈閲?
     */
    val weight: Int = 100,
    /**
     * 鏄惁涓衡€渦p鈥濓紵
     */
    val guaranteed: Boolean = false,
) {
    /**
     * 濂栧搧缁勫唴濂栧搧
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
 * 濂栧搧绛夌骇
 */
@Serializable
data class PrizeLevel(
    /**
     * 濂栧搧绛夌骇
     */
    val level: Int,
    /**
     * 璇ョ瓑绾ц鎶戒腑鐨勬潈閲嶏紙鐢ㄤ簬璺ㄧ瓑绾ч€夋嫨锛?
     */
    val weight: Int,
    /**
     * 鎵€鏈夌粍
     */
    val groups: List<PrizeGroup>,
) {

    constructor(level: Int, weight: Int, vararg groups: PrizeGroup) : this(level, weight, groups.toList())

    /**
     * up姹?
     */
    val guaranteedGroups by lazy { groups.filter { it.guaranteed } }

    /**
     * 鏅€氭睜
     */
    val sharedGroups by lazy { groups.filter { !it.guaranteed } }

    /**
     * 鏄惁 up 姹?
     */
    val isUpPrizePool by lazy { guaranteedGroups.isNotEmpty() }

    /**
     * up閮ㄥ垎鎬绘潈閲?
     */
    val guaranteedWeight by lazy { guaranteedGroups.sumOf { it.weight } }

    /**
     * 鍏变韩閮ㄥ垎鎬绘潈閲?
     */
    val sharedWeight by lazy { sharedGroups.sumOf { it.weight } }
}

/**
 * 濂栨睜
 */
@Serializable
data class PrizePool(
    /**
     *
     */
    val id: String,
    /**
     * 濂栨睜鍚嶇О
     */
    val name: String,
    /**
     * 濂栨睜鎻忚堪
     */
    val description: String,
    /**
     * 鎶藉浠锋牸
     */
    val price: Int,
    /**
     * 濂栨睜鐗╁搧
     */
    val levels: List<PrizeLevel>,
    /**
     * 淇濆簳娆℃暟
     */
    val endTime: Int = 0,

    /**
     * 鏄惁鍏变韩淇濆簳
     */
    val shareEnd: Boolean = false,

    /**
     * 褰撳墠鍗℃睜鎶藉娆℃暟
     */
    val shareEndTime: Int = 0,

    /**
     * 淇濆簳濂栧搧
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
 * 鎶藉缁撴灉
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
 * 鎶藉涓婁笅鏂?
 */
data class RaffleContext(
    /**
     * 濂栨睜
     */
    val pool: PrizePool,
    /**
     * 鐢ㄦ埛鎶藉淇℃伅
     */
    val userRaffle: UserRaffleDto,
    /**
     * 鎶藉缇?
     */
    val group: Group,
)


/**
 * 瀵逛换鎰?List<T> 鎵╁睍涓€涓?weightedRandom 鏂规硶
 * @param weightSelector: 鍑芥暟锛岀敤浜庢彁鍙栨瘡涓厓绱犵殑鏉冮噸
 * @param random: 闅忔満鏁扮敓鎴愬櫒锛堝彲閫夛紝渚夸簬娴嬭瘯锛?
 * @return 闅忔満閫変腑鐨勫厓绱狅紝鎴?null锛堝鏋滃垪琛ㄤ负绌猴級
 */
private fun <T> List<T>.weightedRandom(
    random: Random = Random,
    weightSelector: (T) -> Int,
): T {
    if (isEmpty()) throw RaffleException("濂栨睜閿欒,闅忔満鎶藉閿欒!")

    if (size == 1) return first()

    // 1. 璁＄畻鎬绘潈閲?
    val totalWeight = sumOf { weightSelector(it).toLong() }

    // 2. 濡傛灉鎬绘潈閲?<= 0锛岄€€鍖栦负鏅€氶殢鏈?
    if (totalWeight <= 0) return this.random(random)

    // 3. 鐢熸垚 [0, totalWeight) 鐨勯殢鏈烘暟
    var accumulatedWeight = random.nextLong(totalWeight)

    // 4. 閬嶅巻鍒楄〃锛屽噺鍘绘潈閲嶏紝鐩村埌鈥滃懡涓€?
    for (item in this) {
        //褰撳墠鍏冪礌鏉冮噸
        val weight = weightSelector(item).toLong()
        //鍛戒腑鏁版槸鍚﹀皬浜庡綋鍓嶅厓绱犳潈閲?
        if (accumulatedWeight < weight) {
            return item
        }
        //鍑忓幓鏉冮噸
        accumulatedWeight -= weight
    }

    // 5. 鐞嗚涓婁笉浼氳蛋鍒拌繖閲岋紝浣嗛槻姝㈡诞鐐?鏁存暟璇樊
    return last()
}

/**
 * 濂栧搧宸ュ叿绫?
 */
@OptIn(ConsoleExperimentalApi::class)
object PrizesUtil {
    /**
     * 鍗曟娊
     */
    fun PrizePool.draw(context: RaffleContext): RaffleResult {
        return drawing(context)
    }

    /**
     * 鍗佽繛
     */
    fun PrizePool.drawTen(context: RaffleContext): List<RaffleResult> {
        val result = mutableListOf<RaffleResult>()
        for (i in 1..10) {
            result.add(drawing(context))
        }
        return result
    }

    /**
     * 鎶藉鎿嶄綔
     */
    private fun drawing(context: RaffleContext): RaffleResult {
        val rafflePool = context.pool
        val userRaffle = context.userRaffle
        val userId = userRaffle.id.takeIf { it != 0L } ?: error("閿欒,鎶藉浜篿d涓嶅瓨鍦?")

        //妫€鏌ヤ繚搴?
        if (rafflePool.endTime != 0) {

            val shareEndHit = rafflePool.shareEnd && rafflePool.shareEndTime + 1 == rafflePool.endTime
            val userEndHit = userRaffle.poolTimes[rafflePool.id]?.let { it + 1 == rafflePool.endTime } ?: false

            if (shareEndHit || userEndHit) {
                val level = rafflePool.endPrize ?: throw RaffleException("濂栨睜閰嶇疆淇濆簳锛屼絾濂栨睜鏃犱繚搴曞鍝?")

                val groups = if (level.isUpPrizePool) {
                    level.guaranteedGroups
                } else {
                    level.sharedGroups
                }

                val group =
                    groups.weightedRandom { it.weight }

                val prize = group.getPrize()

                if (prize.getStock() != -1 && prize.tryTake()) {
                    throw RaffleException("闄愰噺濂栨病璐т簡!")
                }

                val raffleResult = RaffleResult(prize, level.level, context.group.id, userId, rafflePool)
                saveRaffleBatch(RaffleType.SINGLE, listOf(raffleResult))
                return raffleResult
            }
        }

        val level = rafflePool.levels.weightedRandom { it.weight }
        val group = level.groups.weightedRandom { it.weight }

        val prize = group.getPrize()

        val raffleResult = RaffleResult(prize, level.level, context.group.id, userId, rafflePool)
        saveRaffleBatch(RaffleType.SINGLE, listOf(raffleResult))
        return raffleResult
    }

    private fun saveRaffleBatch(type: RaffleType, results: List<RaffleResult>) {
        raffleBatchProxy.save(
            RaffleBatchDto(
                userId = results.first().userId,
                groupId = results.first().groupId,
                poolId = results.first().pool.id,
                raffleType = type.name,
                createTime = System.currentTimeMillis(),
                recordCount = results.size,
                records = results
            )
        )
    }

    private val raffleBatchProxy
        get() = EntityProxyRegistry.get<RaffleBatchDto>("raffle") ?: error("抽奖批次代理器未初始化")
}
