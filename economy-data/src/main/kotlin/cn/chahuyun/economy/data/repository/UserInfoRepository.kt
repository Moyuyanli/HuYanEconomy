package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.entity.v2.user.UserEntity

/**
 * 用户核心信息数据持久化层。
 */
object UserInfoRepository {

    @JvmStatic
    fun findById(id: String): UserInfo? =
        HibernateDataStore.selectOneById(UserInfo::class.java, id)

    @JvmStatic
    fun findByQq(qq: Long): UserInfo? =
        HibernateDataStore.selectOne(UserInfo::class.java, "qq", qq)

    @JvmStatic
    fun listAll(): List<UserInfo> =
        HibernateDataStore.selectList(UserInfo::class.java)

    @JvmStatic
    fun save(entity: UserInfo): UserInfo =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteById(id: String): Boolean {
        val entity = findById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findV2ById(id: Long): UserEntity? =
        HibernateDataStore.selectOneById(UserEntity::class.java, id)

    @JvmStatic
    fun findV2ByQq(qq: Long): UserEntity? =
        HibernateDataStore.selectOne(UserEntity::class.java, "qq", qq)

    @JvmStatic
    fun findV2ByUserKey(userKey: String): UserEntity? =
        HibernateDataStore.selectOne(UserEntity::class.java, "userKey", userKey)

    @JvmStatic
    fun listAllV2(): List<UserEntity> =
        HibernateDataStore.selectList(UserEntity::class.java)

    @JvmStatic
    fun saveV2(entity: UserEntity): UserEntity =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteV2ByIdOrQq(idOrQq: Long): Boolean {
        val entity = findV2ById(idOrQq) ?: findV2ByQq(idOrQq) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }
}
