package cn.chahuyun.economy.repair

import cn.chahuyun.economy.entity.UserBackpack
import cn.chahuyun.economy.entity.fish.FishPond
import cn.chahuyun.economy.entity.fish.FishRanking
import cn.chahuyun.economy.entity.props.PropsData
import cn.chahuyun.economy.entity.rob.RobInfo
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.json.JSONUtil
import java.sql.Connection

interface Repair {
    fun repair(): Boolean
}

object RepairManager {
    @JvmStatic
    fun init(): String {
        if (!FishPondRepair().repair()) return "鱼塘错误数据修复失败!"
        if (!RobRepair().repair()) return "抢劫错误数据修复失败!"
        if (!PropRepair().repair()) return "道具系统版本升级失败!"
        return "数据修复与版本升级完成"
    }
}

class FishPondRepair : Repair {
    override fun repair(): Boolean {
        val fishPondSet: MutableSet<FishPond> = mutableSetOf()
        val fishPonds = HibernateFactory.selectList(FishPond::class.java)
        for (pond in fishPonds) {
            if (!fishPondSet.contains(pond)) fishPondSet.add(pond)
        }
        val fishRanks = HibernateFactory.selectList(FishRanking::class.java)
        for (rank in fishRanks) {
            val fishPond = rank.fishPond
            if (fishPondSet.any { it.id == fishPond.id }) continue
            val find = fishPondSet.find { it.code == fishPond.code }
            HibernateFactory.getSessionFactory()?.fromTransaction {
                it.createNativeQuery(
                    "update FishRanking set `FishPondId` = :fishId where id = :id",
                    FishRanking::class.java
                )
                    .setParameter("fishId", find?.id)
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
    override fun repair(): Boolean {
        val columnsToDrop = listOf("isInJail", "cooldown", "lastRobTime", "cooling", "type")
        HibernateFactory.getSessionFactory()?.fromTransaction { session ->
            session.doWork { connection: Connection ->
                for (column in columnsToDrop) {
                    try {
                        connection.createStatement().executeUpdate("ALTER TABLE RobInfo DROP COLUMN $column;")
                    } catch (e: Exception) { /* 忽略不存在的列 */
                    }
                }
            }
        }
        HibernateFactory.selectList(RobInfo::class.java).filter { it.nowTime == null }
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
        val propsDataList = HibernateFactory.selectList(PropsData::class.java)

        for (propsData in propsDataList) {
            try {
                val rawJson = propsData.data ?: continue
                val jsonObject = JSONUtil.parseObj(rawJson)

                // --- 1. 执行字段命名迁移 (旧 -> 新) ---
                if (jsonObject.containsKey("expire")) {
                    val expireValue = jsonObject.get("expire")
                    jsonObject.set("expireDays", expireValue)
                    jsonObject.remove("expire")
                }

                // --- 2. 尝试映射到新类 ---
                val kind = propsData.kind ?: jsonObject.getStr("kind") ?: continue
                val propClass = PropsManager.shopClass(kind) ?: continue

                // 使用修正后的 JSON 反序列化出对象
                val prop = JSONUtil.toBean(jsonObject, propClass)

                // --- 3. 利用 PropsManager 同步核心列数据 ---
                // 这里调用 PropsManager.serialization 会自动填充 propsData 的列（num, expiredTime, status）
                val newPropsData = PropsManager.serialization(prop)
                newPropsData.id = propsData.id

                HibernateFactory.merge(newPropsData)

            } catch (e: Exception) {
                cn.chahuyun.economy.utils.Log.error("升级道具数据失败: id=${propsData.id}", e)
            }
        }

        // 4. 清理残留的无效背包条目
        HibernateFactory.selectList(UserBackpack::class.java).forEach { backpack ->
            if (PropsManager.getProp(backpack) == null) {
                HibernateFactory.delete(backpack)
            }
        }
        return true
    }
}
