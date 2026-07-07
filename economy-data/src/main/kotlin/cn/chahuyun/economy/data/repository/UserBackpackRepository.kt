package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.UserBackpack
import cn.chahuyun.economy.entity.v2.user.UserBackpackEntity

/**
 * 用户背包数据持久化层。
 */
object UserBackpackRepository {

    @JvmStatic
    fun findById(id: Long): UserBackpack? =
        HibernateDataStore.selectOneById(UserBackpack::class.java, id)

    @JvmStatic
    fun listAll(): List<UserBackpack> =
        HibernateDataStore.selectList(UserBackpack::class.java)

    @JvmStatic
    fun save(entity: UserBackpack): UserBackpack =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteById(id: Long): Boolean {
        val entity = findById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findV2ById(id: Long): UserBackpackEntity? =
        HibernateDataStore.selectOneById(UserBackpackEntity::class.java, id)

    @JvmStatic
    fun listAllV2(): List<UserBackpackEntity> =
        HibernateDataStore.selectList(UserBackpackEntity::class.java)

    @JvmStatic
    fun findV2ByIdentity(userKey: String, propCode: String, propKind: String, propId: Long): UserBackpackEntity? =
        HibernateDataStore.selectList(UserBackpackEntity::class.java, "propId", propId)
            .firstOrNull { it.userKey == userKey && it.propCode == propCode && it.propKind == propKind }

    @JvmStatic
    fun saveV2(entity: UserBackpackEntity): UserBackpackEntity =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteV2ById(id: Long): Boolean {
        val entity = findV2ById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }
}
