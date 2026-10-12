package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.fishing.UserFishingInfoEntity

/**
 * 用户钓鱼配置与统计（`hye_user_fishing_info`）数据访问层。
 *
 * 每个用户唯一一条记录；[rodLevel] 为 0 表示尚未购买鱼竿。
 */
object UserFishingInfoRepository {

    @JvmStatic
    fun findByUserId(userId: Long): UserFishingInfoEntity? =
        HibernateDataStore.selectOne(UserFishingInfoEntity::class.java, "userId", userId)

    /** 查询用户钓鱼配置，不存在时创建一条“未购买鱼竿”的空记录。 */
    @JvmStatic
    fun getOrCreate(userId: Long): UserFishingInfoEntity =
        findByUserId(userId) ?: save(UserFishingInfoEntity(userId = userId, rodLevel = 0))

    @JvmStatic
    fun listAll(): List<UserFishingInfoEntity> =
        HibernateDataStore.selectList(UserFishingInfoEntity::class.java)

    @JvmStatic
    fun save(entity: UserFishingInfoEntity): UserFishingInfoEntity =
        HibernateDataStore.merge(entity)
}
