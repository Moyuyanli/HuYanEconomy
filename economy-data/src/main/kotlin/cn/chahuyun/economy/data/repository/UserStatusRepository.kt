package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.UserStatus
import cn.chahuyun.economy.entity.v2.user.UserStatusEntity

/**
 * 用户状态数据持久化层。
 */
object UserStatusRepository {

    @JvmStatic
    fun findById(id: Long): UserStatus? =
        HibernateDataStore.selectOneById(UserStatus::class.java, id)

    @JvmStatic
    fun listAll(): List<UserStatus> =
        HibernateDataStore.selectList(UserStatus::class.java)

    @JvmStatic
    fun save(entity: UserStatus): UserStatus =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteById(id: Long): Boolean {
        val entity = findById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findV2ByUserId(userId: Long): UserStatusEntity? =
        HibernateDataStore.selectOne(UserStatusEntity::class.java, "userId", userId)

    @JvmStatic
    fun listAllV2(): List<UserStatusEntity> =
        HibernateDataStore.selectList(UserStatusEntity::class.java)

    @JvmStatic
    fun saveV2(entity: UserStatusEntity): UserStatusEntity {
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
