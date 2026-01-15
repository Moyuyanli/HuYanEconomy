package cn.chahuyun.economy.utils

import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.SingleMessage
import java.util.regex.Pattern

/**
 * 公共工具
 */
object ShareUtils {

    /**
     * 替换字符串中的变量
     */
    @JvmStatic
    fun replacer(s: String, variable: Array<String>, vararg content: Any?): String {
        var result = s
        for (i in variable.indices) {
            result = result.replace(variable[i], content[i].toString())
        }
        return result
    }

    /**
     * 获取消息中的at人
     * 包括成功at消息和[@xxx]消息
     */
    @JvmStatic
    fun getAtMember(event: GroupMessageEvent): Member? {
        val message: MessageChain = event.message
        for (singleMessage: SingleMessage in message) {
            if (singleMessage is At) {
                return event.group[singleMessage.target]
            }
        }
        val content = message.contentToString()
        val matcher = Pattern.compile("@\\d{5,11}").matcher(content)
        if (matcher.find()) {
            return event.group[matcher.group().substring(1).toLong()]
        }
        return null
    }

    /**
     * 四舍五入后，保留一位小数
     */
    @JvmStatic
    fun rounding(value: Double): Double {
        return kotlin.math.round(value * 10.0) / 10.0
    }

    /**
     * 将百分比的Double转换为int(*100)
     */
    @JvmStatic
    fun percentageToInt(value: Double): Int {
        return kotlin.math.round(value * 100).toInt()
    }

    /**
     * 随机比较
     */
    @JvmStatic
    fun randomCompare(probability: Int): Boolean {
        require(!(probability < 0 || probability > 100)) { "概率只有0~100" }
        val randomed = RandomUtil.randomInt(1, 101)
        return randomed <= probability
    }
}
