package cn.chahuyun.economy.repository

import cn.chahuyun.economy.converter.v1.FishRankingV1Converter
import cn.chahuyun.economy.entity.fish.FishInfo
import cn.chahuyun.economy.entity.fish.FishPond
import cn.chahuyun.economy.entity.fish.FishRanking
import cn.chahuyun.economy.model.fish.FishDto
import cn.chahuyun.economy.model.fish.FishPondDto
import cn.chahuyun.economy.model.fish.FishRankingDto
import cn.chahuyun.hibernateplus.HibernateFactory
import java.util.*

/**
 * 钓鱼相关持久化层（封装 HibernateFactory 访问）。
 *
 * 备注：为了减少文件数量，这里把 FishInfo/FishPond/FishRanking 的简单操作合在一起。
 */
object FishRepository {

    private val rankingConverter = FishRankingV1Converter()

    @JvmStatic
    fun listGroupPonds(): List<FishPond> = HibernateFactory.selectList(FishPond::class.java, "pondType", 1)

    @JvmStatic
    fun saveFishInfo(info: FishInfo): FishInfo = HibernateFactory.merge(info)

    @JvmStatic
    fun saveRanking(
        qq: Long,
        name: String,
        dimensions: Int,
        money: Double,
        fishRodLevel: Int,
        fish: FishDto,
        fishPond: FishPondDto,
    ): FishRankingDto {
        val ranking = FishRanking().apply {
            this.qq = qq
            this.name = name
            this.dimensions = dimensions
            this.money = money
            this.fishRodLevel = fishRodLevel
            this.date = Date()
            this.fish = HibernateFactory.selectOneById(cn.chahuyun.economy.entity.fish.Fish::class.java, fish.id)
            this.fishPond = HibernateFactory.selectOneById(FishPond::class.java, fishPond.id)
        }
        return rankingConverter.toDto(HibernateFactory.merge(ranking))
    }

    @JvmStatic
    fun topRankingByMoney(limit: Int = 10): List<FishRankingDto> =
        HibernateFactory.selectList(FishRanking::class.java)
            .sortedByDescending { it.money }
            .take(limit)
            .map { rankingConverter.toDto(it) }

    @JvmStatic
    fun topRankingWinner(): FishRankingDto? =
        HibernateFactory.selectOneByHql(
            FishRanking::class.java,
            "from FishRanking order by money desc limit 1",
            HashMap<String, Any>()
        )?.let { rankingConverter.toDto(it) }

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


