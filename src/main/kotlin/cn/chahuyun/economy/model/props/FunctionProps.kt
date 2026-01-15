package cn.chahuyun.economy.model.props

import cn.chahuyun.economy.config.EconomyConfig
import cn.chahuyun.economy.constant.PropConstant
import cn.chahuyun.economy.constant.PropConstant.RED_EYES_CD
import cn.chahuyun.economy.plugin.FactorManager
import cn.chahuyun.economy.prop.AbstractProp
import cn.chahuyun.economy.prop.Stackable
import cn.chahuyun.economy.prop.Usable
import cn.chahuyun.economy.prop.UseResult
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.ShareUtils
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.MemberPermission
import java.util.*

/**
 * 功能性道具 (Kotlin 重构版)
 */
class FunctionProps(
    kind: String = "function",
    code: String = "",
    name: String = "",
) : AbstractProp(kind, code, name), Usable, Stackable {

    companion object {
        const val ELECTRIC_BATON = "baton"
        const val RED_EYES = "red-eyes"
        const val MUTE_1 = "mute-1"
        const val MUTE_30 = "mute-30"
    }

    var enableTime: Date? = null
    var electricity: Int = 100
    var muteTime: Int = 0

    override var num: Int = 1
    override var unit: String = "个"
    override var isStack: Boolean = true

    /**
     * 使用后是否消耗
     */
    override var isConsumption: Boolean = false

    override suspend fun use(event: UseEvent): UseResult {
        return when (code) {
            RED_EYES -> {
                val factor = FactorManager.getUserFactor(event.userInfo)
                val buff = factor.getBuffValue(RED_EYES)
                if (buff == null) {
                    factor.setBuffValue(RED_EYES, DateUtil.now())
                    FactorManager.merge(factor)
                    UseResult.success("你猛猛炫了一瓶红牛!")
                } else {
                    val parse = DateUtil.parse(buff)
                    val between = DateUtil.between(parse, Date(), DateUnit.MINUTE)
                    if (between > PropConstant.RED_EYES_CD) {
                        factor.setBuffValue(RED_EYES, DateUtil.now())
                        FactorManager.merge(factor)
                        UseResult.success("续上一瓶红牛!")
                    } else {
                        UseResult.fail("红牛喝多了可对肾不好!")
                    }
                }
            }

            ELECTRIC_BATON -> {
                if (electricity >= 5) {
                    electricity -= 5
                    UseResult.success("使用了电棒，电量剩余 $electricity%")
                } else {
                    UseResult.fail("电棒没电了!")
                }
            }

            MUTE_1, MUTE_30 -> {
                val subject = event.subject
                if (subject is Group) {
                    if (subject.botPermission != MemberPermission.MEMBER) {
                        if (EconomyConfig.unableToUseMuteGroup.contains(subject.id)) {
                            return UseResult.fail("该群禁言功能被禁用!")
                        }

                        subject.sendMessage("请输入你想要禁言的人")
                        val messageEvent =
                            MessageUtil.INSTANCE.nextUserForGroupMessageEventSync(subject.id, event.sender.id, 180)
                        if (messageEvent != null) {
                            val member = ShareUtils.getAtMember(messageEvent)
                            if (member != null) {
                                member.mute(muteTime * 60)
                                return UseResult.success("禁言卡使用成功！")
                            }
                        }
                    }
                }
                UseResult.fail("使用失败!")
            }

            else -> UseResult.fail("该道具无法直接使用!")
        }
    }

    override fun toShopInfo(): String {
        return when (code) {
            RED_EYES -> "道具名称: $name\n价格: $cost 金币\n持续时间: $RED_EYES_CD 分钟\n描述: $description"
            ELECTRIC_BATON -> "道具名称: $name\n价格: $cost 金币\n电量: $electricity%\n描述: $description"
            else -> "道具名称: $name\n价格: $cost 金币\n描述: $description"
        }
    }

    override fun toString(): String {
        return when (code) {
            RED_EYES -> "道具名称: $name\n道具数量: ${if (isStack) "${this.num} ${this.unit}" else 1}\n持续时间: $RED_EYES_CD 分钟\n描述: $description"
            ELECTRIC_BATON -> "道具名称: $name\n道具数量: ${if (isStack) "${this.num} ${this.unit}" else 1}\n电量: $electricity%\n描述: $description"
            else -> super.toString()
        }
    }
}
