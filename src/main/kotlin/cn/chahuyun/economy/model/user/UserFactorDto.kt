package cn.chahuyun.economy.model.user

import cn.hutool.json.JSONUtil
import kotlinx.serialization.Serializable

/**
 * 用户战斗因子DTO
 *
 * 记录用户的战斗属性（抢劫/战斗系统使用）
 */
@Serializable
data class UserFactorDto(
    /** 记录ID */
    val id: Long = 0,
    /** 暴躁因子 */
    val irritable: Double = 0.3,
    /** 力量因子 */
    val force: Double = 0.1,
    /** 闪避因子 */
    val dodge: Double = 0.1,
    /** 抵抗因子 */
    val resistance: Double = 0.3,
    /** 增益效果（JSON数组） */
    val buff: String = "[]"
) {
    fun setBuffValue(buffName: String, value: String?): UserFactorDto {
        val array = JSONUtil.parseArray(buff)
        var foundIndex = -1
        for (i in 0 until array.size) {
            val obj = array.getJSONObject(i)
            if (buffName == obj.getStr("name")) {
                foundIndex = i
                break
            }
        }

        if (value == null) {
            if (foundIndex != -1) array.remove(foundIndex)
        } else {
            if (foundIndex != -1) {
                array.getJSONObject(foundIndex).set("value", value)
            } else {
                array.add(JSONUtil.createObj().set("name", buffName).set("value", value))
            }
        }
        return copy(buff = array.toString())
    }

    fun getBuffValue(buffName: String): String? {
        val array = JSONUtil.parseArray(buff)
        for (obj in array.jsonIter()) {
            if (buffName == obj.getStr("name")) return obj.getStr("value")
        }
        return null
    }
}
