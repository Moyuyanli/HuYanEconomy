package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.raffle.RaffleBatch
import cn.chahuyun.economy.entity.v2.raffle.RaffleBatchEntity

/**
 * 抽奖批次数据持久化层。
 */
object RaffleBatchRepository {

    @JvmStatic
    fun findById(id: Long): RaffleBatch? =
        HibernateDataStore.selectOneById(RaffleBatch::class.java, id)

    @JvmStatic
    fun listAll(): List<RaffleBatch> =
        HibernateDataStore.selectList(RaffleBatch::class.java)

    @JvmStatic
    fun save(entity: RaffleBatch): RaffleBatch =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteById(id: Long): Boolean {
        val entity = findById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findV2ById(id: Long): RaffleBatchEntity? =
        HibernateDataStore.selectOneById(RaffleBatchEntity::class.java, id)

    @JvmStatic
    fun listAllV2(): List<RaffleBatchEntity> =
        HibernateDataStore.selectList(RaffleBatchEntity::class.java)

    @JvmStatic
    fun saveV2(entity: RaffleBatchEntity): RaffleBatchEntity =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteV2ById(id: Long): Boolean {
        val entity = findV2ById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }
}
