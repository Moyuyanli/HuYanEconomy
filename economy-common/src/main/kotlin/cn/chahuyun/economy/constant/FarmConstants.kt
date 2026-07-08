package cn.chahuyun.economy.constant

object FarmConstants {
    /** 农场等级上限。 */
    const val MAX_LEVEL = 30

    /** 单个玩家最多可拥有的地块数量。 */
    const val MAX_PLOTS = 18

    /** 新玩家初始解锁的地块数量。 */
    const val INITIAL_UNLOCKED_PLOTS = 6

    /** 地块状态：未解锁。 */
    const val PLOT_LOCKED = "LOCKED"

    /** 地块状态：已解锁但未种植。 */
    const val PLOT_EMPTY = "EMPTY"

    /** 地块状态：已种植作物。 */
    const val PLOT_PLANTED = "PLANTED"

    /** 背包物品类型：种子。 */
    const val ITEM_SEED = "SEED"

    /** 背包物品类型：果实。 */
    const val ITEM_FRUIT = "FRUIT"
}
