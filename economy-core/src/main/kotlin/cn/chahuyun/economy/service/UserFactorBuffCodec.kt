package cn.chahuyun.economy.service

import cn.chahuyun.economy.model.user.UserFactorDto
import cn.hutool.json.JSONUtil

object UserFactorBuffCodec {

    fun getBuffValue(factor: UserFactorDto, buffName: String): String? {
        val array = JSONUtil.parseArray(factor.buff)
        for (obj in array.jsonIter()) {
            if (buffName == obj.getStr("name")) return obj.getStr("value")
        }
        return null
    }

    fun withBuffValue(factor: UserFactorDto, buffName: String, value: String?): UserFactorDto {
        val array = JSONUtil.parseArray(factor.buff)
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
        return factor.copy(buff = array.toString())
    }
}
