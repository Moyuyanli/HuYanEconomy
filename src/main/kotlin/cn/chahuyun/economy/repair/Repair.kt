@file:Suppress("SqlResolve", "DEPRECATION")

package cn.chahuyun.economy.repair

import cn.chahuyun.economy.entity.UserBackpack
import cn.chahuyun.economy.entity.fish.FishPond
import cn.chahuyun.economy.entity.fish.FishRanking
import cn.chahuyun.economy.entity.props.PropsData
import cn.chahuyun.economy.entity.rob.RobInfo
import cn.chahuyun.economy.prop.BaseProp
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.prop.PropsManager.destroy
import cn.chahuyun.economy.prop.Stackable
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.json.JSONUtil
import java.sql.Connection
import java.util.Locale.getDefault


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

            val id = fishPond?.id ?: error("错误,背包中道具id为空!")
            var find = fishPondSet.find { it.id == id }
            if (find != null) {
                continue
            }

            find = fishPondSet.find { it.code == fishPond.code }

            HibernateFactory.getSessionFactory().fromTransaction {
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
        HibernateFactory.getSessionFactory().fromTransaction { session ->
            session.doWork { connection: Connection ->
                for (column in columnsToDrop) {
                    val sql = "ALTER TABLE RobInfo DROP COLUMN $column;"
                    try {
                        with(connection.createStatement()) {
                            executeUpdate(sql)
                        }
                    } catch (_: Exception) {
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
    /**
     * 修复
     * 执行道具数据的修复操作，包括字段迁移、数量修复、堆叠物品合并等
     *
     * @return 修复操作是否成功完成，成功返回true，失败返回false
     */
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

                // 修复道具数量为0的情况
                if (jsonObject.containsKey("num")) {
                    if (jsonObject.getInt("num") == 0) {
                        jsonObject["num"] = 1
                    }
                }

                // --- 尝试映射到新类 ---
                var kind = propsData.kind ?: jsonObject.getStr("kind") ?: continue
                kind = kind.uppercase(getDefault())
                jsonObject["kind"] = kind
                val propClass = PropsManager.getPropClass(kind) ?: continue

                if (jsonObject.containsKey("code") || jsonObject.getStr("code")
                        .isNullOrBlank() || jsonObject.getStr("code") == "code"
                ) {
                    jsonObject["code"] = HibernateFactory.selectOne<UserBackpack>("propId", propsData.id!!)?.propCode
                }

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
        val userBackpackToBaseProp = mutableMapOf<Pair<String, String>, UserBackpack>()
        for_backpack@ for (backpack in backpackList) {
            //取道具code
            val propCode = if (backpack.propCode != null) {
                backpack.propCode
            } else {
                backpack.destroy()
                continue
            }

            if (backpack.propId == null) {
                backpack.destroy()
                continue
            }

            val template = PropsManager.getTemplate<BaseProp>(propCode)

            if (template is Stackable && template.isStack) {
                val userId = backpack.userId
                if (userId == null) {
                    backpack.destroy()
                    continue
                }
                val key = userId to propCode
                if (userBackpackToBaseProp.containsKey(key)) {
                    val only = userBackpackToBaseProp[key]
                    val one = (only?.getPropLegacy() ?: run {
                        only?.destroy()
                        userBackpackToBaseProp.remove(key)
                        null
                    }) ?: continue@for_backpack

                    val prop = backpack.getPropLegacy()
                    if (prop !is Stackable) {
                        backpack.destroy()
                        continue@for_backpack
                    }
                    val num = if (prop.num <= 0) 1 else prop.num

                    //可堆叠物品数量合并
                    if (one is Stackable) {
                        one.num += num
                    }

                    val updatedPropsData = PropsManager.serialization(one)
                    updatedPropsData.id = only?.propId

                    HibernateFactory.merge(updatedPropsData)
                    backpack.destroy()
                } else {
                    userBackpackToBaseProp[key] = backpack
                }
            } else {
                continue
            }
        }

        // 3. 最终清理残留的无效背包条目
        HibernateFactory.selectList(UserBackpack::class.java).forEach { backpack ->
            try {
                if (PropsManager.getProp(backpack) == null) {
                    backpack.destroy()
                }
            } catch (_: Exception) {
                try {
                    backpack.destroy()
                } catch (_: Exception) { /* 忽略 */
                }
            }
        }
        return true
    }
}
