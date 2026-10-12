package cn.chahuyun.economy.constant

/**
 * 鱼塘等级常量。
 *
 * 新版钓鱼模型中，鱼塘归属用户，[level] 决定可钓到的鱼的等级上限，
 * [FishPondLevelConstant] 记录“升到该等级”所需的金币与最低鱼竿等级。
 *
 * @author Moyuyanli
 */
enum class FishPondLevelConstant(
    /** 升到该等级所需金币。 */
    val amount: Int,
    /** 升到该等级所需的最低鱼竿等级。 */
    val minRodLevel: Int,
) {
    LV_2(5000, 0),
    LV_3(10000, 0),
    LV_4(15000, 0),
    LV_5(20000, 0),
    LV_6(25000, 0),
    LV_7(30000, 0),
    LV_8(50000, 10),
    LV_9(70000, 20),
    LV_10(100000, 30);

    companion object {
        /** 鱼塘最高等级。 */
        const val MAX_LEVEL: Int = 10

        /** 鱼塘初始等级。 */
        const val MIN_LEVEL: Int = 1

        /** 返回升到 [targetLevel] 所需的等级常量；越界返回 null。 */
        fun of(targetLevel: Int): FishPondLevelConstant? =
            entries.firstOrNull { it.ordinal + 2 == targetLevel }

        fun values(): Array<FishPondLevelConstant> = entries.toTypedArray()
    }
}
