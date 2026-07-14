@file:Suppress("SqlResolve", "DEPRECATION")

package cn.chahuyun.economy.repair

import cn.chahuyun.economy.converter.v1.PropsDataV1Converter
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.data.repository.RepairRepository
import cn.chahuyun.economy.entity.UserBackpack
import cn.chahuyun.economy.entity.fish.FishPond
import cn.chahuyun.economy.prop.BaseProp
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.prop.Stackable
import cn.chahuyun.economy.utils.Log
import cn.hutool.json.JSONUtil
import java.util.Locale.getDefault

interface Repair {

    /**
     * 执行修复。
     */
    fun repair(): Boolean
}

object RepairManager {

    @JvmStatic
    fun usage(): String = buildString {
        appendLine("请指定数据修复范围:")
        appendLine("hye repair v1 - 仅修复 V1 数据")
        appendLine("hye repair v2 - 仅修复 V2 数据")
        append("hye repair V1TOV2 - 从只读 V1 备份修复迁移到 V2 的数据")
    }

    @JvmStatic
    fun init(scope: RepairScope): String = when (scope) {
        RepairScope.V1 -> repairV1()
        RepairScope.V2 -> repairV2()
        RepairScope.V1_TO_V2 -> repairV1ToV2()
    }

    private fun repairV1(): String {
        if (!FishPondRepair().repair()) {
            return "V1 鱼塘错误数据修复失败!"
        }
        if (!RobRepair().repair()) {
            return "V1 抢劫错误数据修复失败!"
        }
        if (!PropRepair().repair()) {
            return "V1 道具错误数据修复失败!"
        }
        return "V1 数据修复完成"
    }

    private fun repairV2(): String {
        return "V2 当前没有已登记的修复动作，未修改任何数据"
    }

    private fun repairV1ToV2(): String {
        val signRepair = UserSignRepair()
        if (!signRepair.repair()) {
            return "V1 到 V2 连续签到信息修复失败!"
        }
        val fishPondMigration = EntityProxyRegistry.migrateModuleTo("fish_pond", DataVersion.V2, switchAfterSuccess = true)
        if (!fishPondMigration.success) {
            return "V1 到 V2 鱼塘实体迁移修复失败: ${fishPondMigration.errors.take(3).joinToString("; ")}"
        }
        val propMigration = EntityProxyRegistry.migrateModuleTo("props", DataVersion.V2, switchAfterSuccess = true)
        if (!propMigration.success) {
            return "V1 到 V2 道具实体迁移修复失败: ${propMigration.errors.take(3).joinToString("; ")}"
        }

        return "V1 到 V2 迁移修复完成\n连续签到修复 ${signRepair.repairedCount} 人，鱼塘迁移 ${fishPondMigration.migratedCount} 条，道具迁移 ${propMigration.migratedCount} 条"
    }
}

enum class RepairScope {
    V1,
    V2,
    V1_TO_V2
}

class UserSignRepair : Repair {
    var repairedCount: Int = 0
        private set

    override fun repair(): Boolean = try {
        repairedCount = RepairRepository.repairUserSignData()
        true
    } catch (e: Exception) {
        Log.error("修复连续签到信息失败", e)
        false
    }
}

class FishPondRepair : Repair {

    /**
     * 修复鱼塘重复引用数据。
     */
    override fun repair(): Boolean {
        // 唯一鱼塘集合。
        val fishPondSet: MutableSet<FishPond> = mutableSetOf()
        // 现有鱼塘。
        val fishPonds = RepairRepository.listFishPonds()

        // 收集所有唯一鱼塘。
        for (pond in fishPonds) {
            if (!fishPondSet.contains(pond)) {
                fishPondSet.add(pond)
            }
        }

        // 所有钓鱼排行信息。
        val fishRanks = RepairRepository.listFishRankings()
        for (rank in fishRanks) {
            val fishPond = rank.fishPond

            val id = fishPond?.id ?: error("错误, 鱼塘 id 为空!")
            var find = fishPondSet.find { it.id == id }
            if (find != null) {
                continue
            }

            find = fishPondSet.find { it.code == fishPond.code }

            RepairRepository.updateFishRankingPond(rank.id, find?.id)
        }

        for (pond in fishPonds) {
            if (fishPondSet.find { it.id == pond.id } != null) continue
            RepairRepository.delete(pond)
        }

        return true
    }
}

class RobRepair : Repair {
    /**
     * 修复抢劫旧字段和无效记录。
     */
    override fun repair(): Boolean {
        RepairRepository.dropRobLegacyColumns()

        // 删除 nowTime 为空的 RobInfo 记录。
        RepairRepository.listRobInfos()
            .filter { it.nowTime == null }
            .forEach { RepairRepository.delete(it) }

        return true
    }
}

/**
 * 道具系统版本迁移 (v1 -> v2)。
 * 将旧版扁平 JSON 升级为结构化存储。
 */
class PropRepair : Repair {
    private val propsDataConverter = PropsDataV1Converter()

    private fun UserBackpack.destroy() {
        val propId = propId ?: return
        RepairRepository.deletePropsData(propId)
        RepairRepository.delete(this)
    }

    private fun UserBackpack.getV1Prop(): BaseProp? {
        val propId = propId ?: return null
        val propsData = RepairRepository.findPropsData(propId) ?: return null
        val dto = propsDataConverter.toDto(propsData)
        val clazz = PropsManager.getPropClass(dto.kind) ?: return null
        return PropsManager.deserialization(dto, clazz)
    }

    private fun saveV1PropsData(prop: BaseProp, id: Long) {
        val dto = PropsManager.serialization(prop).copy(id = id)
        RepairRepository.savePropsData(propsDataConverter.toEntity(dto))
    }

    /**
     * 执行道具数据修复，包括字段迁移、数量修复、可堆叠物品合并等。
     *
     * @return 修复操作是否成功完成
     */
    override fun repair(): Boolean {
        // 1. 同步 PropsData 的列数据。
        val propsDataList = RepairRepository.listPropsData()

        for (propsData in propsDataList) {
            try {
                val rawJson = propsData.data ?: continue
                val jsonObject = JSONUtil.parseObj(rawJson)

                // 执行字段命名迁移: expire -> expireDays。
                if (jsonObject.containsKey("expire")) {
                    val expireValue = jsonObject.get("expire")
                    jsonObject.set("expireDays", expireValue)
                    jsonObject.remove("expire")
                }

                // 修复道具数量为 0 的情况。
                if (jsonObject.containsKey("num")) {
                    if (jsonObject.getInt("num") == 0) {
                        jsonObject["num"] = 1
                    }
                }

                // 尝试映射到新的道具类型。
                var kind = propsData.kind ?: jsonObject.getStr("kind") ?: continue
                kind = kind.uppercase(getDefault())
                jsonObject["kind"] = kind
                val propClass = PropsManager.getPropClass(kind) ?: continue

                val codeStr = jsonObject.getStr("code")
                if (!jsonObject.containsKey("code") || codeStr.isNullOrBlank() || codeStr == "code") {
                    val fallback = RepairRepository.findBackpackPropCodeByPropId(propsData.id!!)
                    if (!fallback.isNullOrBlank()) {
                        jsonObject["code"] = fallback
                    }
                }

                // 使用修正后的 JSON 反序列化出对象。
                val prop = JSONUtil.toBean(jsonObject, propClass)
                // 利用 PropsManager 同步核心列数据。
                saveV1PropsData(prop, propsData.id ?: 0)
            } catch (e: Exception) {
                Log.error("升级道具数据失败: id=${propsData.id}", e)
            }
        }

        // 2. 可堆叠物品合并修复。
        val backpackList = RepairRepository.listUserBackpacks()
        val userBackpackToBaseProp = mutableMapOf<Pair<String, String>, UserBackpack>()
        for_backpack@ for (backpack in backpackList) {
            // 读取道具 code。
            if (backpack.propCode == null) {
                backpack.destroy()
                continue
            }
            val propCode = backpack.propCode!!

            if (backpack.propId == null) {
                backpack.destroy()
                continue
            }

            val template = try {
                PropsManager.getTemplate<BaseProp>(propCode)
            } catch (e: Exception) {
                Log.error("修复背包道具失败: 未找到道具模板 code=$propCode, backpackId=${backpack.id}", e)
                backpack.destroy()
                continue@for_backpack
            }

            if (template is Stackable && template.isStack) {
                val userId = backpack.userId
                if (userId == null) {
                    backpack.destroy()
                    continue
                }
                val key = userId to propCode
                if (userBackpackToBaseProp.containsKey(key)) {
                    val only = userBackpackToBaseProp[key]
                    val one = (only?.getV1Prop() ?: run {
                        only?.destroy()
                        userBackpackToBaseProp.remove(key)
                        null
                    }) ?: continue@for_backpack

                    val prop = backpack.getV1Prop()
                    if (prop !is Stackable) {
                        backpack.destroy()
                        continue@for_backpack
                    }
                    val num = if (prop.num <= 0) 1 else prop.num

                    // 可堆叠物品数量合并。
                    if (one is Stackable) {
                        one.num += num
                    }

                    saveV1PropsData(one, only?.propId ?: 0)
                    backpack.destroy()
                } else {
                    userBackpackToBaseProp[key] = backpack
                }
            } else {
                continue
            }
        }

        // 3. 最终清理残留的无效背包条目。
        RepairRepository.listUserBackpacks().forEach { backpack ->
            try {
                if (backpack.getV1Prop() == null) {
                    backpack.destroy()
                }
            } catch (_: Exception) {
                try {
                    backpack.destroy()
                } catch (_: Exception) { /* 忽略清理失败 */
                }
            }
        }
        return true
    }
}
