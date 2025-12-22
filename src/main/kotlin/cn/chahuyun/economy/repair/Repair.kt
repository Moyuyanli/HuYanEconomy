@file:Suppress("SqlResolve")

package cn.chahuyun.economy.repair

import cn.chahuyun.economy.entity.UserBackpack
import cn.chahuyun.economy.entity.fish.FishPond
import cn.chahuyun.economy.entity.fish.FishRanking
import cn.chahuyun.economy.entity.props.PropsData
import cn.chahuyun.economy.entity.rob.RobInfo
import cn.chahuyun.economy.prop.BaseProp
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.prop.Stackable
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.json.JSONUtil
import java.sql.Connection


interface Repair {

    /**
     * 修复
     */
    fun repair(): Boolean


}

object RepairManager {

    @JvmStatic
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

        return "数据修复与版本升级完成"
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

            HibernateFactory.getSessionFactory()?.fromTransaction {
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
        HibernateFactory.getSessionFactory()?.fromTransaction { session ->
            session.doWork { connection: Connection ->
                for (column in columnsToDrop) {
                    val sql = "ALTER TABLE RobInfo DROP COLUMN $column;"
                    try {
                        with(connection.createStatement()) {
                            executeUpdate(sql)
                        }
                    } catch (e: Exception) {
                        // 忽略
                    }
                }
            }
        }

        // 删除 nowTime 为空的 RobInfo 记录
        HibernateFactory.selectList(RobInfo::class.java)
            .filter { it.nowTime == null }
            .forEach { HibernateFactory.delete(it) }

        return true
    }

}


/**
 * 道具系统版本迁移 (v1 -> v2)
 * 将旧版扁平 JSON 升级为结构化存储
 */
class PropRepair : Repair {
    override fun repair(): Boolean {
        // 1. 同步 PropsData 的列数据
        val propsDataList = HibernateFactory.selectList(PropsData::class.java)

        for (propsData in propsDataList) {
            try {
                val rawJson = propsData.data ?: continue
                val jsonObject = JSONUtil.parseObj(rawJson)

                // --- 执行字段命名迁移 (旧 -> 新) ---
                if (jsonObject.containsKey("expire")) {
                    val expireValue = jsonObject.get("expire")
                    jsonObject.set("expireDays", expireValue)
                    jsonObject.remove("expire")
                }

                // --- 尝试映射到新类 ---
                val kind = propsData.kind ?: jsonObject.getStr("kind") ?: continue
                val propClass = PropsManager.shopClass(kind) ?: continue
                
                // 使用修正后的 JSON 反序列化出对象
                val prop = JSONUtil.toBean(jsonObject, propClass)

                // --- 利用 PropsManager 同步核心列数据 ---
                val updatedPropsData = PropsManager.serialization(prop)
                updatedPropsData.id = propsData.id
                
                HibernateFactory.merge(updatedPropsData)

            } catch (e: Exception) {
                cn.chahuyun.economy.utils.Log.error("升级道具数据失败: id=${propsData.id}", e)
            }
        }

        // 2. 堆叠物品合并修复
        val backpackList = HibernateFactory.selectList(UserBackpack::class.java)
        val stackMap = mutableMapOf<Pair<String, String>, BaseProp>()

        for (backpack in backpackList) {
            try {
                val prop = PropsManager.getProp(backpack) ?: continue
                if (prop is Stackable && prop.isStack) {
                    val key = backpack.userId to backpack.propCode
                    if (stackMap.containsKey(key)) {
                        val base = stackMap[key]!! as Stackable
                        val currentNum = if (prop.num <= 0) 1 else prop.num
                        base.num += currentNum
                        
                        // 销毁重复的道具数据
                        PropsManager.destroyProsInBackpack(backpack.propId)
                        // 将这个重复的背包项标记为待删除（或者直接删除）
                        HibernateFactory.delete(backpack)
                        
                        // 更新主道具的数据（找到 stackMap 中的那个）
                        // 注意：这里需要找到 stackMap 中对应的那个 propId 对应的背包
                        // 为了简化，我们直接在循环结束后统一更新，或者这里找到主背包
                        val mainBackpack = backpackList.find { it.userId == key.first && it.propCode == key.second && it.id != backpack.id }
                        if (mainBackpack != null) {
                            PropsManager.updateProp(mainBackpack.propId, base as BaseProp)
                        }
                    } else {
                        stackMap[key] = prop
                    }
                }
            } catch (e: Exception) {
                try {
                    HibernateFactory.delete(backpack)
                } catch (ex: Exception) {
                    // 忽略
                }
            }
        }

        // 3. 最终清理残留的无效背包条目
        HibernateFactory.selectList(UserBackpack::class.java).forEach { backpack ->
            try {
                if (PropsManager.getProp(backpack) == null) {
                    HibernateFactory.delete(backpack)
                }
            } catch (e: Exception) {
                try {
                    HibernateFactory.delete(backpack)
                } catch (ex: Exception) { /* 忽略 */ }
            }
        }
        return true
    }
}
