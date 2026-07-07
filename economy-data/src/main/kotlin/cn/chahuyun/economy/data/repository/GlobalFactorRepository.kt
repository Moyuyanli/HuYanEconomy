package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.GlobalFactor
import cn.chahuyun.economy.entity.v2.GlobalFactorEntity

/**
 * 全局因子数据持久化层。
 */
object GlobalFactorRepository {

    @JvmStatic
    fun findById(id: Int): GlobalFactor? =
        HibernateDataStore.selectOneById(GlobalFactor::class.java, id)

    @JvmStatic
    fun listAll(): List<GlobalFactor> =
        HibernateDataStore.selectList(GlobalFactor::class.java)

    @JvmStatic
    fun save(entity: GlobalFactor): GlobalFactor =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteById(id: Int): Boolean {
        val entity = findById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findV2ById(id: Long): GlobalFactorEntity? =
        HibernateDataStore.selectOneById(GlobalFactorEntity::class.java, id)

    @JvmStatic
    fun listAllV2(): List<GlobalFactorEntity> =
        HibernateDataStore.selectList(GlobalFactorEntity::class.java)

    @JvmStatic
    fun saveV2(entity: GlobalFactorEntity): GlobalFactorEntity =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteV2ById(id: Long): Boolean {
        val entity = findV2ById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }
}
