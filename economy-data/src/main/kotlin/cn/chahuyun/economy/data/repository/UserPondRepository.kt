package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.fishing.UserPondEntity

/**
 * 用户鱼塘（`hye_user_pond`）数据访问层。
 *
 * 新版模型中鱼塘归属用户，同一用户下鱼塘名称唯一。
 */
object UserPondRepository {

    @JvmStatic
    fun findById(id: Long): UserPondEntity? =
        HibernateDataStore.selectOneById(UserPondEntity::class.java, id)

    @JvmStatic
    fun listByUser(userId: Long): List<UserPondEntity> =
        HibernateDataStore.selectList(UserPondEntity::class.java, "userId", userId)

    @JvmStatic
    fun findByUserAndName(userId: Long, name: String): UserPondEntity? =
        HibernateDataStore.selectOne(UserPondEntity::class.java, mapOf("userId" to userId, "name" to name))

    @JvmStatic
    fun listAll(): List<UserPondEntity> =
        HibernateDataStore.selectList(UserPondEntity::class.java)

    @JvmStatic
    fun save(entity: UserPondEntity): UserPondEntity =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun delete(entity: UserPondEntity) =
        HibernateDataStore.delete(entity)
}
