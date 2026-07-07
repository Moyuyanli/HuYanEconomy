package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.TitleInfo
import cn.chahuyun.economy.entity.v2.title.TitleInfoEntity

/**
 * 称号信息数据持久化层。
 */
object TitleInfoRepository {

    @JvmStatic
    fun findById(id: Int): TitleInfo? =
        HibernateDataStore.selectOneById(TitleInfo::class.java, id)

    @JvmStatic
    fun listAll(): List<TitleInfo> =
        HibernateDataStore.selectList(TitleInfo::class.java)

    @JvmStatic
    fun save(entity: TitleInfo): TitleInfo =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteById(id: Int): Boolean {
        val entity = findById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findV2ById(id: Long): TitleInfoEntity? =
        HibernateDataStore.selectOneById(TitleInfoEntity::class.java, id)

    @JvmStatic
    fun listAllV2(): List<TitleInfoEntity> =
        HibernateDataStore.selectList(TitleInfoEntity::class.java)

    @JvmStatic
    fun saveV2(entity: TitleInfoEntity): TitleInfoEntity =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteV2ById(id: Long): Boolean {
        val entity = findV2ById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }
}
