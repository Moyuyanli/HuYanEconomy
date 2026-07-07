package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.rob.RobInfo
import cn.chahuyun.economy.entity.v2.rob.RobInfoEntity

/**
 * 抢劫信息数据持久化层。
 */
object RobInfoRepository {

    @JvmStatic
    fun findById(id: Long): RobInfo? =
        HibernateDataStore.selectOneById(RobInfo::class.java, id)

    @JvmStatic
    fun listAll(): List<RobInfo> =
        HibernateDataStore.selectList(RobInfo::class.java)

    @JvmStatic
    fun save(entity: RobInfo): RobInfo =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteById(id: Long): Boolean {
        val entity = findById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findV2ByUserId(userId: Long): RobInfoEntity? =
        HibernateDataStore.selectOne(RobInfoEntity::class.java, "userId", userId)

    @JvmStatic
    fun listAllV2(): List<RobInfoEntity> =
        HibernateDataStore.selectList(RobInfoEntity::class.java)

    @JvmStatic
    fun saveV2(entity: RobInfoEntity): RobInfoEntity {
        val existing = findV2ByUserId(entity.userId)
        val now = System.currentTimeMillis()
        if (existing != null) {
            entity.id = existing.id
            entity.createdAt = existing.createdAt
        }
        if (entity.createdAt == 0L) entity.createdAt = now
        entity.updatedAt = now
        return HibernateDataStore.merge(entity)
    }

    @JvmStatic
    fun deleteV2ByUserId(userId: Long): Boolean {
        val entity = findV2ByUserId(userId) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }
}
