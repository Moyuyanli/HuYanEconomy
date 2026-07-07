@file:Suppress("SqlResolve", "DEPRECATION")

package cn.chahuyun.economy.repair

import cn.chahuyun.economy.converter.v1.UserBackpackV1Converter
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
    private val userBackpackConverter = UserBackpackV1Converter()

    private fun UserBackpack.destroy() {
        val propId = propId ?: return
        PropsManager.destroyPros(propId)
        RepairRepository.delete(this)
    }

    private fun UserBackpack.asDto() = userBackpackConverter.toDto(this)

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
                val updatedPropsData = PropsManager.serialization(prop).copy(id = propsData.id ?: 0)

                PropsManager.savePropsData(updatedPropsData)
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
                    val one = (only?.let { PropsManager.getProp(it.asDto()) } ?: run {
                        only?.destroy()
                        userBackpackToBaseProp.remove(key)
                        null
                    }) ?: continue@for_backpack

                    val prop = PropsManager.getProp(backpack.asDto())
                    if (prop !is Stackable) {
                        backpack.destroy()
                        continue@for_backpack
                    }
                    val num = if (prop.num <= 0) 1 else prop.num

                    // 可堆叠物品数量合并。
                    if (one is Stackable) {
                        one.num += num
                    }

                    val updatedPropsData = PropsManager.serialization(one).copy(id = only?.propId ?: 0)

                    PropsManager.savePropsData(updatedPropsData)
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
                if (PropsManager.getProp(backpack.asDto()) == null) {
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
