package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.fishing.FishEntity

/**
 * 全局鱼类配置（`hye_fish`）数据访问层。
 *
 * 鱼类配置是全局静态数据，不关联用户；钓鱼结算时读取配置并写入快照到流水表。
 */
object FishRepository {

    @JvmStatic
    fun listAll(): List<FishEntity> =
        HibernateDataStore.selectList(FishEntity::class.java)

    @JvmStatic
    fun findById(id: Long): FishEntity? =
        HibernateDataStore.selectOneById(FishEntity::class.java, id)

    @JvmStatic
    fun findByName(name: String): FishEntity? =
        HibernateDataStore.selectOne(FishEntity::class.java, "name", name)

    @JvmStatic
    fun listByLevel(level: Int): List<FishEntity> =
        HibernateDataStore.selectList(FishEntity::class.java, "level", level)

    @JvmStatic
    fun count(): Long =
        listAll().size.toLong()

    @JvmStatic
    fun save(entity: FishEntity): FishEntity =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun saveAll(entities: Collection<FishEntity>): List<FishEntity> =
        entities.map { HibernateDataStore.merge(it) }

    @JvmStatic
    fun delete(entity: FishEntity) =
        HibernateDataStore.delete(entity)
}
