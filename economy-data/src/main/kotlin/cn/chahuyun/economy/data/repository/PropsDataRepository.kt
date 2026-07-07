package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.props.PropsData
import cn.chahuyun.economy.entity.v2.props.PropsDataEntity

/**
 * 道具数据持久化层。
 */
object PropsDataRepository {

    @JvmStatic
    fun findById(id: Long): PropsData? =
        HibernateDataStore.selectOneById(PropsData::class.java, id)

    @JvmStatic
    fun listAll(): List<PropsData> =
        HibernateDataStore.selectList(PropsData::class.java)

    @JvmStatic
    fun save(entity: PropsData): PropsData =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteById(id: Long): Boolean {
        val entity = findById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findV2ById(id: Long): PropsDataEntity? =
        HibernateDataStore.selectOneById(PropsDataEntity::class.java, id)

    @JvmStatic
    fun listAllV2(): List<PropsDataEntity> =
        HibernateDataStore.selectList(PropsDataEntity::class.java)

    @JvmStatic
    fun saveV2(entity: PropsDataEntity): PropsDataEntity =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteV2ById(id: Long): Boolean {
        val entity = findV2ById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }
}
