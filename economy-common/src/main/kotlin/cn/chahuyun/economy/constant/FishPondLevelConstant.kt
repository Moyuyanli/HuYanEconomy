package cn.chahuyun.economy.constant

/**
 * 鱼塘等级常量
 *
 * @author Moyuyanli
 * @date 2024/9/7 18:50
 */
enum class FishPondLevelConstant(val amount: Int, val minFishLevel: Int) {
    /** 升到 2 级鱼塘需要 5000 金币。 */
    LV_2(5000, 0),

    /** 升到 3 级鱼塘需要 10000 金币。 */
    LV_3(10000, 0),

    /** 升到 4 级鱼塘需要 15000 金币。 */
    LV_4(15000, 0),

    /** 升到 5 级鱼塘需要 20000 金币。 */
    LV_5(20000, 0),

    /** 升到 6 级鱼塘需要 25000 金币。 */
    LV_6(25000, 0),

    /** 升到 7 级鱼塘需要 30000 金币。 */
    LV_7(30000, 0),

    /** 升到 8 级鱼塘需要 50000 金币，且钓竿等级至少 10 级。 */
    LV_8(50000, 10),

    /** 升到 9 级鱼塘需要 70000 金币，且钓竿等级至少 20 级。 */
    LV_9(70000, 20),

    /** 升到 10 级鱼塘需要 100000 金币，且钓竿等级至少 30 级。 */
    LV_10(100000, 30); // 修复了 Java 版本中 LV_10 是 10000 的可能错误（应为 100000?）

    companion object {
        /** 鱼塘最高等级。 */
        const val MAX_LEVEL = 10

        fun values(): Array<FishPondLevelConstant> = entries.toTypedArray()
    }
}

