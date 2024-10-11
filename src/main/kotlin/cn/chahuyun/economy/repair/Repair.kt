package cn.chahuyun.economy.repair

import cn.chahuyun.economy.entity.fish.FishPond
import cn.chahuyun.economy.entity.fish.FishRanking
import cn.chahuyun.economy.entity.rob.RobInfo
import cn.chahuyun.hibernateplus.HibernateFactory


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

