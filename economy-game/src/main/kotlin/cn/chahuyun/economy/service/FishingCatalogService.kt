package cn.chahuyun.economy.service

import cn.chahuyun.economy.data.repository.FishRepository
import cn.chahuyun.economy.entity.fishing.FishEntity
import cn.chahuyun.economy.utils.Log
import java.math.BigDecimal

/**
 * 鱼类配置（`hye_fish`）目录服务。
 *
 * 首次启动配置表为空时写入一份默认鱼类目录，之后全部从数据库读取，
 * 保证钓鱼结算依赖的是持久化配置而不是硬编码在业务里的临时数据。
 */
object FishingCatalogService {

    @Volatile
    private var initialized = false

    fun initCatalog() {
        if (initialized) return
        if (FishRepository.count() == 0L) {
            val defaults = defaultFish()
            FishRepository.saveAll(defaults)
            Log.info("钓鱼:初始化默认鱼类配置 ${defaults.size} 条")
        }
        initialized = true
    }

    fun all(): List<FishEntity> = FishRepository.listAll()

    fun levelFishList(level: Int): List<FishEntity> = FishRepository.listByLevel(level)

    /** 取指定鱼等级区间内的鱼，用于按鱼塘等级抽样。 */
    fun levelRangeFish(minLevel: Int, maxLevel: Int): List<FishEntity> =
        all().filter { it.level in minLevel..maxLevel }

    private fun defaultFish(): List<FishEntity> = listOf(
        fish(1, "鲫鱼", "最常见的淡水鱼，皮实好养。", "5", 5, 30, 5, 12, 20, 30, 1),
        fish(1, "白条", "水面成群的小鱼，手感很轻。", "3", 4, 22, 4, 10, 16, 22, 1),
        fish(2, "鲤鱼", "体态丰满，力气不小。", "12", 15, 60, 15, 28, 45, 60, 2),
        fish(2, "草鱼", "食量惊人，拉扯感十足。", "15", 20, 80, 20, 40, 60, 80, 2),
        fish(3, "鲈鱼", "肉质紧实，是钓友的心头好。", "40", 20, 90, 20, 40, 70, 90, 3),
        fish(3, "鳜鱼", "名贵淡水鱼，警惕性很高。", "60", 15, 70, 15, 35, 55, 70, 3),
        fish(4, "金枪鱼", "深海猛物，需要足够耐心。", "150", 50, 200, 50, 90, 150, 200, 4),
        fish(4, "三文鱼", "逆流而上的强者。", "200", 40, 160, 40, 80, 120, 160, 4),
        fish(5, "中华鲟", "水中活化石，极其稀有。", "800", 80, 300, 80, 150, 230, 300, 5, special = true),
        fish(5, "蓝鳍金枪鱼", "传说中的顶级渔获。", "1200", 100, 350, 100, 180, 260, 350, 5, special = true),
    )

    private fun fish(
        level: Int,
        name: String,
        description: String,
        price: String,
        dimensionsMin: Int,
        dimensionsMax: Int,
        dimensions1: Int,
        dimensions2: Int,
        dimensions3: Int,
        dimensions4: Int,
        technique: Int,
        special: Boolean = false,
    ): FishEntity = FishEntity(
        level = level,
        name = name,
        description = description,
        price = BigDecimal(price),
        dimensionsMin = dimensionsMin,
        dimensionsMax = dimensionsMax,
        dimensions1 = dimensions1,
        dimensions2 = dimensions2,
        dimensions3 = dimensions3,
        dimensions4 = dimensions4,
        technique = technique,
        special = special,
    )
}
