package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.UserRaffle
import cn.chahuyun.economy.entity.v2.user.UserRaffleEntity

/**
 * 用户抽奖数据持久化层。
 */
object UserRaffleRepository {

    @JvmStatic
    fun findById(id: Long): UserRaffle? =
        HibernateDataStore.getSessionFactory().fromTransaction { session ->
            session.find(UserRaffle::class.java, id)?.also { it.poolTimes.size }
        }

    @JvmStatic
    fun listAll(): List<UserRaffle> =
        HibernateDataStore.getSessionFactory().fromTransaction { session ->
            session.createQuery("from UserRaffle", UserRaffle::class.java).resultList
                .onEach { it.poolTimes.size }
        }

    @JvmStatic
    fun save(raffle: UserRaffle): UserRaffle =
        HibernateDataStore.merge(raffle)

    @JvmStatic
    fun deleteById(id: Long): Boolean {
        val entity = HibernateDataStore.selectOneById(UserRaffle::class.java, id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findV2ByUserId(userId: Long): UserRaffleEntity? =
        HibernateDataStore.selectOne(UserRaffleEntity::class.java, "userId", userId)

    @JvmStatic
    fun listAllV2(): List<UserRaffleEntity> =
        HibernateDataStore.selectList(UserRaffleEntity::class.java)

    @JvmStatic
    fun saveV2(entity: UserRaffleEntity): UserRaffleEntity {
        val existing = HibernateDataStore.selectOne(UserRaffleEntity::class.java, "userId", entity.userId)
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
        val entity = HibernateDataStore.selectOne(UserRaffleEntity::class.java, "userId", userId) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }
}
