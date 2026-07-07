package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.UserBackpack
import cn.chahuyun.economy.entity.fish.FishPond
import cn.chahuyun.economy.entity.fish.FishRanking
import cn.chahuyun.economy.entity.props.PropsData
import cn.chahuyun.economy.entity.rob.RobInfo
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

    fun findBackpackPropCodeByPropId(propId: Long): String? =
        HibernateDataStore.selectOne<UserBackpack>("propId", propId)?.propCode

    fun listUserBackpacks(): List<UserBackpack> =
        HibernateDataStore.selectList(UserBackpack::class.java)

    fun delete(entity: Any) {
        HibernateDataStore.delete(entity)
    }
}
