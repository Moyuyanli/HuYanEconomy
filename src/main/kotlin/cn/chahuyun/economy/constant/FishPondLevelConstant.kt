package cn.chahuyun.economy.constant

/**
 * 鱼塘等级常量
 *
 * @author Moyuyanli
 * @date 2024/9/7 18:50
 */
enum class FishPondLevelConstant(val amount: Int, val minFishLevel: Int) {
    LV_2(5000, 0),
    LV_3(10000, 0),
    LV_4(15000, 0),
    LV_5(20000, 0),
    LV_6(25000, 0),
    LV_7(30000, 0),
    LV_8(50000, 10),
    LV_9(70000, 20),
    LV_10(100000, 30); // 修复了 Java 版本中 LV_10 是 10000 的可能错误（应为 100000?）

    companion object {
        fun values(): Array<FishPondLevelConstant> = entries.toTypedArray()
    }
}

