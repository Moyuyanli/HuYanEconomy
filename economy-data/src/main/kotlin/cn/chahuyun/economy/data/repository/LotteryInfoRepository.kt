package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.LotteryInfo
import cn.chahuyun.economy.entity.v2.LotteryInfoEntity

/**
 * 彩票信息数据持久化层。
 */
object LotteryInfoRepository {

    @JvmStatic
    fun findById(id: Int): LotteryInfo? =
        HibernateDataStore.selectOneById(LotteryInfo::class.java, id)

    @JvmStatic
    fun listAll(): List<LotteryInfo> =
        HibernateDataStore.selectList(LotteryInfo::class.java)

    @JvmStatic
    fun save(entity: LotteryInfo): LotteryInfo =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteById(id: Int): Boolean {
        val entity = findById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findV2ById(id: Long): LotteryInfoEntity? =
        HibernateDataStore.selectOneById(LotteryInfoEntity::class.java, id)

    @JvmStatic
    fun listAllV2(): List<LotteryInfoEntity> =
        HibernateDataStore.selectList(LotteryInfoEntity::class.java)

    @JvmStatic
    fun saveV2(entity: LotteryInfoEntity): LotteryInfoEntity =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteV2ById(id: Long): Boolean {
        val entity = findV2ById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }
}
