package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.redpack.RedPack
import cn.chahuyun.economy.entity.v2.redpack.RedPackEntity

/**
 * 红包持久化层。
 */
object RedPackRepository {

    @JvmStatic
    fun findById(id: Int): RedPack? = HibernateDataStore.selectOneById(id)

    @JvmStatic
    fun listByGroupId(groupId: Long): List<RedPack> =
        HibernateDataStore.selectList(RedPack::class.java, "groupId", groupId)

    @JvmStatic
    fun listAll(): List<RedPack> = HibernateDataStore.selectList(RedPack::class.java)

    @JvmStatic
    fun save(pack: RedPack): RedPack = HibernateDataStore.merge(pack)

    @JvmStatic
    fun delete(pack: RedPack) {
        HibernateDataStore.delete(pack)
    }

    @JvmStatic
    fun deleteById(id: Int): Boolean {
        val entity = findById(id) ?: return false
        delete(entity)
        return true
    }

    @JvmStatic
    fun findV2ById(id: Long): RedPackEntity? =
        HibernateDataStore.selectOneById(RedPackEntity::class.java, id)

    @JvmStatic
    fun listAllV2(): List<RedPackEntity> =
        HibernateDataStore.selectList(RedPackEntity::class.java)

    @JvmStatic
    fun saveV2(entity: RedPackEntity): RedPackEntity {
        val existing = if (entity.id != 0L) findV2ById(entity.id) else null
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
    fun deleteV2ById(id: Long): Boolean {
        val entity = findV2ById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }
}


