package cn.chahuyun.economy

/**
 * 奖品
 */
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
     * 奖品元数据
     * 如: propsId=123, count=5
     */
    val metadata: Map<String, Any> = emptyMap(),
    /**
     * 奖品库存
     * -1 表示无限库存
     */
    val stock: Int = -1
)

/**
 * 奖品组
 */
data class PrizeGroup(
    /**
     * 奖品组名称
     */
    val name: String,
    /**
     * 奖品组描述
     */
    val description: String,
    /**
     * 奖品组内奖品
     */
    val prizes: List<Prize>,
    /**
     * 该组在所属层级中的权重
     */
    val weight: Int,
    /**
     * 是否组内均分概率
     */
    val shared: Boolean = true,
    /**
     * 是否为“独占堆”？
     */
    val guaranteed: Boolean = false
)

/**
 * 奖品等级
 */
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
     * 所有堆
     */
    val groups: List<PrizeGroup>
) {
    // 所有“独占堆”
    val guaranteedGroups by lazy { groups.filter { it.guaranteed } }

    // 共享堆（非独占）
    val sharedGroups by lazy { groups.filter { !it.guaranteed } }

    // 独占部分总权重
    val guaranteedWeight by lazy { guaranteedGroups.sumOf { it.weight } }

    // 共享部分总权重
    val sharedWeight by lazy { sharedGroups.sumOf { it.weight } }
}

/**
 * 奖池
 */
data class RafflePool(
    /**
     *
     */
    val id: String,
    /**
     * 奖池code
     */
    val code: String,
    /**
     * 奖池名称
     */
    val name: String,
    /**
     * 奖池描述
     */
    val description: String,
    /**
     * 奖池等级
     */
    val levels: List<PrizeLevel>,
    /**
     * 保底次数
     */
    val endTime: Int = 0,
    /**
     * 保底奖品
     */
    val endPrize: List<Prize> = emptyList()
)

/**
 * 抽奖结果
 */
data class RaffleResult(
    val prize: Prize,
    val level: Int,
    val groupId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)