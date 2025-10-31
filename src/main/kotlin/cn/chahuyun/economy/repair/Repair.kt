package cn.chahuyun.economy.repair

import cn.chahuyun.economy.entity.UserBackpack
import cn.chahuyun.economy.entity.fish.FishPond
import cn.chahuyun.economy.entity.fish.FishRanking
import cn.chahuyun.economy.entity.rob.RobInfo
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.hibernateplus.HibernateFactory
import java.sql.Connection


interface Repair {

    /**
     * 修复
     */
    fun repair(): Boolean


}

object RepairManager {

    fun init(): String {
        if (!FishPondRepair().repair()) {
            return "鱼塘错误数据修复失败!"
        }
        if (!RobRepair().repair()) {
            return "抢劫错误数据修复失败!"
        }
        if (!PropRepair().repair()) {
            return "道具错误数据修复失败!"
        }

        return "修复完成"
    }

}

class FishPondRepair() : Repair {

    /**
     * 修复
     */
    override fun repair(): Boolean {
        //唯一鱼塘
        val fishPondSet: MutableSet<FishPond> = mutableSetOf()
        //现有鱼塘
        val fishPonds = HibernateFactory.selectList(FishPond::class.java)

        //取所有唯一鱼塘
        for (pond in fishPonds) {
            if (!fishPondSet.contains(pond)) {
                fishPondSet.add(pond)
            }
        }

        //所有钓鱼信息
        val fishRanks = HibernateFactory.selectList(FishRanking::class.java)
        for (rank in fishRanks) {
            val fishPond = rank.fishPond

            var find = fishPondSet.find { it.id == fishPond.id }
            if (find != null) {
                continue
            }

            find = fishPondSet.find { it.code == fishPond.code }

            HibernateFactory.getSession().fromTransaction {
                val createQuery = it.createNativeQuery(
                    "update FishRanking set `FishPondId` = :fishId where id = :id",
                    FishRanking::class.java
                )
                createQuery.setParameter("fishId", find?.id)
                    .setParameter("id", rank.id)
                    .executeUpdate()
            }

        }

        for (pond in fishPonds) {
            if (fishPondSet.find { it.id == pond.id } != null) continue
            HibernateFactory.delete(pond)
        }

        return true
    }


}

class RobRepair : Repair {
    /**
     * 修复
     */
    override fun repair(): Boolean {
        // 定义要删除的列名
        val columnsToDrop = listOf("isInJail", "cooldown", "lastRobTime", "cooling", "type")

        // 在事务中执行每个 ALTER TABLE 语句
        HibernateFactory.getSession().fromTransaction { session ->
            session.doWork { connection: Connection ->
                for (column in columnsToDrop) {
                    val sql = "ALTER TABLE RobInfo DROP COLUMN $column;"
                    try {
                        with(connection.createStatement()) {
                            executeUpdate(sql)
                        }
                    } catch (e: Exception) {
                        println(e.message)
                    }
                }
            }
        }

        // 删除 nowTime 为空的 RobInfo 记录
        HibernateFactory.selectList(RobInfo::class.java).stream()
            .filter { it.nowTime == null }
            .forEach { `object`: RobInfo? -> HibernateFactory.delete(`object`) }

        return true
    }

}


class PropRepair : Repair {
    /**
     * 修复
     */
    override fun repair(): Boolean {
        val list = HibernateFactory.selectList(UserBackpack::class.java)

        val map = mutableMapOf<Pair<Long, String>, UserBackpack>()

        for (userBackpack in list) {
            val key = userBackpack.propId to userBackpack.userId

            if (map.containsKey(key)) {
                HibernateFactory.delete(userBackpack)
                continue
            } else {
                map[key] = userBackpack
            }

            try {
                PropsManager.getProp(userBackpack)
            } catch (e: Exception) {
                HibernateFactory.delete(userBackpack)
            }
        }
        return true
    }

}
