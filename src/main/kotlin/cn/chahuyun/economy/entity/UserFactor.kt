package cn.chahuyun.economy.entity

import cn.hutool.json.JSONUtil
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 用户因子
 *
 * @author Moyuyanli
 * @date 2024/9/26 9:27
 */
@Entity(name = "UserFactor")
@Table
class UserFactor(
    @Id
    var id: Long? = null,

    /**
     * 暴躁值
     * 打他md
     */
    var irritable: Double = 0.3,

    /**
     * 武力值
     * 抢劫成功附加概率
     */
    @Column(name = "`force`")
    var force: Double = 0.1,

    /**
     * 闪避值
     * 各种地方的闪避、逃跑概率
     */
    var dodge: Double = 0.1,

    /**
     * 反抗因子
     * md,跟你爆了！
     */
    var resistance: Double = 0.3,

    /**
     * json存储格式
     */
    var buff: String = "[]"
) {

    /**
     * 设置或更新指定名称的buff的值
     *
     * @param buffName buff名称
     * @param value buff的值
     * @return 当前对象实例，支持链式调用
     */
    fun setBuffValue(buffName: String, value: String): UserFactor {
        val array = JSONUtil.parseArray(this.buff)
        var found = false

        // 尝试找到并更新现有的buff
        for (i in 0 until array.size) {
            val obj = array.getJSONObject(i)
            if (buffName == obj.getStr("name")) {
                obj.set("value", value)
                found = true
                break
            }
        }

        // 如果没有找到，则添加新的buff
        if (!found) {
            val newBuff = JSONUtil.createObj()
                .set("name", buffName)
                .set("value", value)
            array.add(newBuff)
        }

        this.buff = array.toString()
        return this
    }

    /**
     * 获取指定名称的buff的值
     *
     * @param buffName buff名称
     * @return buff的值, 如果不存在则返回null
     */
    fun getBuffValue(buffName: String): String? {
        val array = JSONUtil.parseArray(this.buff)
        for (obj in array.jsonIter()) {
            if (buffName == obj.getStr("name")) {
                return obj.getStr("value")
            }
        }
        return null
    }
}
