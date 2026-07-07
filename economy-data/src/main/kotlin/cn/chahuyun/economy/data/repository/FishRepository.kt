package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.converter.v1.FishRankingV1Converter
import cn.chahuyun.economy.entity.fish.Fish
import cn.chahuyun.economy.entity.fish.FishInfo
import cn.chahuyun.economy.entity.fish.FishPond
import cn.chahuyun.economy.entity.fish.FishRanking
import cn.chahuyun.economy.entity.v2.fish.FishEntity
import cn.chahuyun.economy.entity.v2.fish.FishInfoEntity
import cn.chahuyun.economy.entity.v2.fish.FishPondEntity
import cn.chahuyun.economy.model.fish.FishDto
import cn.chahuyun.economy.model.fish.FishPondDto
import cn.chahuyun.economy.model.fish.FishRankingDto
import java.util.*

/**
 * 钓鱼运行数据持久化层。
 */
object FishRepository {

    private val rankingConverter = FishRankingV1Converter()

    @JvmStatic
    fun findFishById(id: Int): Fish? =
        HibernateDataStore.selectOneById(Fish::class.java, id)

    @JvmStatic
    fun listFish(): List<Fish> =
        HibernateDataStore.selectList(Fish::class.java)

    @JvmStatic
    fun saveFish(fish: Fish): Fish =
        HibernateDataStore.merge(fish)

    @JvmStatic
    fun deleteFishById(id: Int): Boolean {
        val entity = findFishById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findFishV2ById(id: Long): FishEntity? =
        HibernateDataStore.selectOneById(FishEntity::class.java, id)

    @JvmStatic
    fun listFishV2(): List<FishEntity> =
        HibernateDataStore.selectList(FishEntity::class.java)

    @JvmStatic
    fun saveFishV2(fish: FishEntity): FishEntity =
        HibernateDataStore.merge(fish)

    @JvmStatic
    fun deleteFishV2ById(id: Long): Boolean {
        val entity = findFishV2ById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findFishInfoById(id: Long): FishInfo? =
        HibernateDataStore.selectOneById(FishInfo::class.java, id)

    @JvmStatic
    fun findFishInfoByQq(qq: Long): FishInfo? =
        HibernateDataStore.selectOne(FishInfo::class.java, "qq", qq)

    @JvmStatic
    fun listFishInfo(): List<FishInfo> =
        HibernateDataStore.selectList(FishInfo::class.java)

    @JvmStatic
    fun deleteFishInfoById(id: Long): Boolean {
        val entity = findFishInfoById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findFishInfoV2ByQq(qq: Long): FishInfoEntity? =
        HibernateDataStore.selectOne(FishInfoEntity::class.java, "qq", qq)

    @JvmStatic
    fun listFishInfoV2(): List<FishInfoEntity> =
        HibernateDataStore.selectList(FishInfoEntity::class.java)

    @JvmStatic
    fun saveFishInfoV2(info: FishInfoEntity): FishInfoEntity =
        HibernateDataStore.merge(info)

    @JvmStatic
    fun deleteFishInfoV2ByQq(qq: Long): Boolean {
        val entity = findFishInfoV2ByQq(qq) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findFishPondById(id: Int): FishPond? =
        HibernateDataStore.selectOneById(FishPond::class.java, id)

    @JvmStatic
    fun findFishPondByCode(code: String): FishPond? =
        HibernateDataStore.selectOne(FishPond::class.java, "code", code)

    @JvmStatic
    fun listFishPonds(): List<FishPond> =
        HibernateDataStore.selectList(FishPond::class.java)

    @JvmStatic
    fun deleteFishPondById(id: Int): Boolean {
        val entity = findFishPondById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findFishPondV2ById(id: Long): FishPondEntity? =
        HibernateDataStore.selectOneById(FishPondEntity::class.java, id)

    @JvmStatic
    fun findFishPondV2ByCode(code: String): FishPondEntity? =
        HibernateDataStore.selectOne(FishPondEntity::class.java, "code", code)

    @JvmStatic
    fun listFishPondsV2(): List<FishPondEntity> =
        HibernateDataStore.selectList(FishPondEntity::class.java)

    @JvmStatic
    fun saveFishPondV2(pond: FishPondEntity): FishPondEntity =
        HibernateDataStore.merge(pond)

    @JvmStatic
    fun deleteFishPondV2ById(id: Long): Boolean {
        val entity = findFishPondV2ById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun listGroupPonds(): List<FishPond> =
        HibernateDataStore.selectList(FishPond::class.java, "pondType", 1)

    @JvmStatic
    fun saveFishPond(pond: FishPond): FishPond =
        HibernateDataStore.merge(pond)

    @JvmStatic
    fun saveFishInfo(info: FishInfo): FishInfo =
        HibernateDataStore.merge(info)

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
            this.fish = HibernateDataStore.selectOneById(Fish::class.java, fish.id)
            this.fishPond = HibernateDataStore.selectOneById(FishPond::class.java, fishPond.id)
        }
        return rankingConverter.toDto(HibernateDataStore.merge(ranking))
    }

    @JvmStatic
    fun topRankingByMoney(limit: Int = 10): List<FishRankingDto> =
        HibernateDataStore.selectList(FishRanking::class.java)
            .sortedByDescending { it.money }
            .take(limit)
            .map { rankingConverter.toDto(it) }

    @JvmStatic
    fun topRankingWinner(): FishRankingDto? =
        topRankingByMoney(limit = 1).firstOrNull()

    /**
     * 将所有正在钓鱼的状态置回 false。
     */
    @JvmStatic
    fun resetAllFishingStatus(): Boolean =
        // Keep this as one transaction so startup repair is all-or-nothing.
        HibernateDataStore.getSessionFactory().fromTransaction { session ->
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
