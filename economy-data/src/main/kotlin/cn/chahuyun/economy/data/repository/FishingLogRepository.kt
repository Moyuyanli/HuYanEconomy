package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.fishing.FishingLogEntity

/**
 * 钓鱼结果流水（`hye_fishing_log`）数据访问层。
 *
 * 流水是结算事实记录，排行榜等统计均从流水派生，不依赖后续被修改的鱼或鱼塘配置。
 */
object FishingLogRepository {

    @JvmStatic
    fun save(entity: FishingLogEntity): FishingLogEntity =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun listAll(): List<FishingLogEntity> =
        HibernateDataStore.selectList(FishingLogEntity::class.java)

    @JvmStatic
    fun listByUser(userId: Long): List<FishingLogEntity> =
        HibernateDataStore.selectList(FishingLogEntity::class.java, "userId", userId)

    @JvmStatic
    fun listByPond(pondId: Long): List<FishingLogEntity> =
        HibernateDataStore.selectList(FishingLogEntity::class.java, "pondId", pondId)

    @JvmStatic
    fun listByFish(fishId: Long): List<FishingLogEntity> =
        HibernateDataStore.selectList(FishingLogEntity::class.java, "fishId", fishId)

    @JvmStatic
    fun countAll(): Long =
        listAll().size.toLong()

    @JvmStatic
    fun deleteByPond(pondId: Long) =
        listByPond(pondId).forEach { HibernateDataStore.delete(it) }
}
