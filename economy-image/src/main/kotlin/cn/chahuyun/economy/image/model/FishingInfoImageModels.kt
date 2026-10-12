package cn.chahuyun.economy.image.model

/**
 * 钓鱼信息图中的一条键值信息。
 */
data class FishingInfoLine(
    val label: String,
    val value: String,
)

/**
 * 钓鱼信息图输入模型。
 */
data class FishingInfoCard(
    val owner: String,
    val rodLevel: String,
    val currentPond: String,
    val maxFishSize: String,
    val maxNetIncome: String,
    val totalCount: String,
    val biggestFish: String,
    val biggestFishDetail: String,
) {
    /** 主体信息行。 */
    val lines: List<FishingInfoLine>
        get() = listOf(
            FishingInfoLine("鱼竿等级", rodLevel),
            FishingInfoLine("当前鱼塘", currentPond),
            FishingInfoLine("最大鱼尺寸", maxFishSize),
            FishingInfoLine("单条最高收益", maxNetIncome),
            FishingInfoLine("累计钓鱼次数", totalCount),
        )
}
