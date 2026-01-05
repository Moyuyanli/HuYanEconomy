package cn.chahuyun.economy.repository

import cn.chahuyun.economy.entity.fish.FishInfo
import cn.chahuyun.economy.entity.fish.FishPond
import cn.chahuyun.economy.entity.fish.FishRanking
import cn.chahuyun.hibernateplus.HibernateFactory

/**
 * 钓鱼相关持久化层（封装 HibernateFactory 访问）。
 *
 * 备注：为了减少文件数量，这里把 FishInfo/FishPond/FishRanking 的简单操作合在一起。
 */
object FishRepository {

    @JvmStatic
    fun listGroupPonds(): List<FishPond> = HibernateFactory.selectList(FishPond::class.java, "pondType", 1)

    @JvmStatic
    fun saveFishInfo(info: FishInfo): FishInfo = HibernateFactory.merge(info)

    @JvmStatic
    fun saveRanking(ranking: FishRanking): FishRanking = HibernateFactory.merge(ranking)

    @JvmStatic
    fun topRankingByMoney(limit: Int = 10): List<FishRanking> =
        HibernateFactory.selectList(FishRanking::class.java).sortedByDescending { it.money }.take(limit)

    /**
     * 将所有正在钓鱼的状态置回 false（对应原 refresh 逻辑）。
     */
    @JvmStatic
    fun resetAllFishingStatus(): Boolean {
        return HibernateFactory.getSessionFactory().fromTransaction { session ->
            try {
                val builder = session.criteriaBuilder
                val query = builder.createQuery(FishInfo::class.java)
                val from = query.from(FishInfo::class.java)
                query.select(from).where(builder.equal(from.get<Boolean>("status"), true))

                session.createQuery(query).list().forEach {
                    it.status = false
                    session.merge(it)
                }
                true
            } catch (_: Exception) {
                false
            }
        } ?: false
    }
}


