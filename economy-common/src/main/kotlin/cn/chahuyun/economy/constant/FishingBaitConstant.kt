package cn.chahuyun.economy.constant

/**
 * 鱼饵常量配置。
 *
 * 新版钓鱼模型中鱼饵作为可选增益：用户通过 `hye_user_fishing_info.default_bait_item_id`
 * 记录选用的鱼饵 [id]；未选择时使用“空钩”。
 */
enum class FishingBaitConstant(
    /** 鱼饵配置 ID，用于 `default_bait_item_id`。 */
    val id: Long,
    /** 鱼饵稳定编码。 */
    val code: String,
    /** 鱼饵展示名称。 */
    val displayName: String,
    /** 鱼饵等级，影响可钓到的鱼等级下限。 */
    val level: Int,
    /** 鱼饵品质，影响惊喜尺寸概率。 */
    val quality: Float,
    /** 鱼饵价格。 */
    val price: Double,
) {
    BAIT_1(1L, "bait-1", "红虫", 1, 0.01f, 100.0),
    BAIT_2(2L, "bait-2", "蚯蚓", 2, 0.02f, 500.0),
    BAIT_3(3L, "bait-3", "秘制饵料", 3, 0.03f, 2000.0),
    BAIT_L_1(4L, "bait-l-1", "幸运鱼饵", 4, 0.04f, 8000.0),
    BAIT_Q_1(5L, "bait-q-1", "传说鱼饵", 5, 0.05f, 20000.0);

    companion object {
        /** 未选择鱼饵时的占位 ID。 */
        const val EMPTY_ID: Long = 0L

        fun ofId(id: Long?): FishingBaitConstant? =
            entries.firstOrNull { it.id == id }

        fun ofCode(code: String): FishingBaitConstant? =
            entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
    }
}
