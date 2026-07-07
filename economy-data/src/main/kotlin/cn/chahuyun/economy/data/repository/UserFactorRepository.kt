package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.UserFactor
import cn.chahuyun.economy.entity.v2.user.UserFactorEntity

/**
 * 用户因子数据持久化层。
 */
object UserFactorRepository {

    @JvmStatic
    fun findById(id: Long): UserFactor? =
        HibernateDataStore.selectOneById(UserFactor::class.java, id)

    @JvmStatic
    fun listAll(): List<UserFactor> =
        HibernateDataStore.selectList(UserFactor::class.java)

    @JvmStatic
    fun save(entity: UserFactor): UserFactor =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteById(id: Long): Boolean {
        val entity = findById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findV2ByUserId(userId: Long): UserFactorEntity? =
        HibernateDataStore.selectOne(UserFactorEntity::class.java, "userId", userId)

    @JvmStatic
    fun listAllV2(): List<UserFactorEntity> =
        HibernateDataStore.selectList(UserFactorEntity::class.java)

    @JvmStatic
    fun saveV2(entity: UserFactorEntity): UserFactorEntity {
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
