package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.UserBackpack
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.entity.fish.FishPond
import cn.chahuyun.economy.entity.fish.FishRanking
import cn.chahuyun.economy.entity.props.PropsData
import cn.chahuyun.economy.entity.rob.RobInfo
import cn.chahuyun.economy.entity.v2.user.UserEntity
import java.sql.Connection

/**
 * Legacy repair data access facade.
 *
 * Native SQL and DDL are intentionally kept here because these repairs target
 * old physical table/column names that do not map cleanly to current entities.
 */
object RepairRepository {

    private val ROB_LEGACY_COLUMNS = listOf("isInJail", "cooldown", "lastRobTime", "cooling", "type")

    fun listFishPonds(): List<FishPond> =
        HibernateDataStore.selectList(FishPond::class.java)

    fun listFishRankings(): List<FishRanking> =
        HibernateDataStore.selectList(FishRanking::class.java)

    fun updateFishRankingPond(rankId: Int, fishPondId: Int?) {
        HibernateDataStore.getSessionFactory().fromTransaction { session ->
            // Legacy schema uses a physical FishPondId column for this relation.
            val query = session.createNativeQuery(
                "update FishRanking set `FishPondId` = :fishId where id = :id",
                FishRanking::class.java
            )
            query.setParameter("fishId", fishPondId)
                .setParameter("id", rankId)
                .executeUpdate()
        }
    }

    fun dropRobLegacyColumns() {
        HibernateDataStore.getSessionFactory().fromTransaction { session ->
            session.doWork { connection: Connection ->
                for (column in ROB_LEGACY_COLUMNS) {
                    val sql = "ALTER TABLE RobInfo DROP COLUMN $column;"
                    try {
                        connection.createStatement().use { statement ->
                            statement.executeUpdate(sql)
                        }
                    } catch (_: Exception) {
                        // Ignore columns that were already removed by an earlier repair run.
                    }
                }
            }
        }
    }

    fun listRobInfos(): List<RobInfo> =
        HibernateDataStore.selectList(RobInfo::class.java)

    fun listPropsData(): List<PropsData> =
        HibernateDataStore.selectList(PropsData::class.java)

    fun findPropsData(id: Long): PropsData? =
        HibernateDataStore.selectOneById(PropsData::class.java, id)

    fun savePropsData(entity: PropsData): PropsData =
        HibernateDataStore.merge(entity)

    fun deletePropsData(id: Long) {
        findPropsData(id)?.let(HibernateDataStore::delete)
    }

    fun findBackpackPropCodeByPropId(propId: Long): String? =
        HibernateDataStore.selectOne<UserBackpack>("propId", propId)?.propCode

    fun listUserBackpacks(): List<UserBackpack> =
        HibernateDataStore.selectList(UserBackpack::class.java)

    fun repairUserSignData(): Int =
        HibernateDataStore.getSessionFactory().fromTransaction { session ->
            val legacyUsers = session.createQuery("from UserInfo", UserInfo::class.java).resultList
            val currentUsers = session.createQuery("from UserEntityV2", UserEntity::class.java).resultList
                .associateBy { it.qq }

            legacyUsers.count { legacy ->
                val current = currentUsers[legacy.qq] ?: return@count false
                val merged = mergeUserSignData(
                    legacySign = legacy.sign,
                    legacySignTime = legacy.signTime?.time ?: 0L,
                    legacySignNumber = legacy.signNumber,
                    legacyOldSignNumber = legacy.oldSignNumber,
                    currentSign = current.sign,
                    currentSignTime = current.signTime,
                    currentSignNumber = current.signNumber,
                    currentOldSignNumber = current.oldSignNumber
                )
                if (!merged.changed) return@count false

                current.sign = merged.sign
                current.signTime = merged.signTime
                current.signNumber = merged.signNumber
                current.oldSignNumber = merged.oldSignNumber
                current.updatedAt = System.currentTimeMillis()
                session.merge(current)
                true
            }
        }

    fun delete(entity: Any) {
        HibernateDataStore.delete(entity)
    }
}

data class MergedUserSignData(
    val sign: Boolean,
    val signTime: Long,
    val signNumber: Int,
    val oldSignNumber: Int,
    val changed: Boolean
)

fun mergeUserSignData(
    legacySign: Boolean,
    legacySignTime: Long,
    legacySignNumber: Int,
    legacyOldSignNumber: Int,
    currentSign: Boolean,
    currentSignTime: Long,
    currentSignNumber: Int,
    currentOldSignNumber: Int
): MergedUserSignData {
    val signTime = maxOf(legacySignTime, currentSignTime)
    val signNumber = maxOf(legacySignNumber, currentSignNumber)
    val oldSignNumber = maxOf(legacyOldSignNumber, currentOldSignNumber)
    val sign = when {
        legacySignTime > currentSignTime -> legacySign
        currentSignTime > legacySignTime -> currentSign
        else -> legacySign || currentSign
    }
    return MergedUserSignData(
        sign = sign,
        signTime = signTime,
        signNumber = signNumber,
        oldSignNumber = oldSignNumber,
        changed = sign != currentSign || signTime != currentSignTime ||
            signNumber != currentSignNumber || oldSignNumber != currentOldSignNumber
    )
}
