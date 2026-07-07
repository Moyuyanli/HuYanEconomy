package cn.chahuyun.economy.utils

import cn.chahuyun.economy.common.math.Probability
import cn.chahuyun.economy.common.text.TextTemplate
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import java.util.regex.Pattern

object ShareUtils {

    @JvmStatic
    fun replacer(s: String, variable: Array<String>, vararg content: Any?): String {
        return TextTemplate.replace(s, variable, *content)
    }

    @JvmStatic
    fun getAtMember(event: GroupMessageEvent): Member? {
        val message = event.message
        for (singleMessage in message) {
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

    @JvmStatic
    fun rounding(value: Double): Double {
        return Probability.oneDecimal(value)
    }

    @JvmStatic
    fun percentageToInt(value: Double): Int {
        return Probability.percentToInt(value)
    }

    @JvmStatic
    fun randomCompare(probability: Int): Boolean {
        return Probability.hit(probability)
    }
}
