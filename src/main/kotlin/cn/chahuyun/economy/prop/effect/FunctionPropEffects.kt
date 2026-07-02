package cn.chahuyun.economy.prop.effect

import cn.chahuyun.economy.config.EconomyConfig
import cn.chahuyun.economy.constant.PropConstant
import cn.chahuyun.economy.model.props.FunctionProps
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.plugin.FactorManager
import cn.chahuyun.economy.prop.BaseProp
import cn.chahuyun.economy.prop.UseResult
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.ShareUtils
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.contact.MemberPermission
import java.util.*

object RedEyesEffectHandler : PropEffectHandler {
    override val codes: Set<String> = setOf(FunctionProps.RED_EYES)

    override suspend fun use(prop: BaseProp, event: UseEvent): UseResult {
        val factor = FactorManager.getUserFactor(event.userInfo)
        val buff = factor.getBuffValue(FunctionProps.RED_EYES)
        if (buff == null) {
            factor.setBuffValue(FunctionProps.RED_EYES, DateUtil.now())
            FactorManager.merge(factor)
            return UseResult.success("你猛猛炫了一瓶红牛")
        }

        val between = DateUtil.between(DateUtil.parse(buff), Date(), DateUnit.MINUTE)
        if (between <= PropConstant.RED_EYES_CD) {
            return UseResult.fail("红牛喝多了可对肾不好!")
        }

        factor.setBuffValue(FunctionProps.RED_EYES, DateUtil.now())
        FactorManager.merge(factor)
        return UseResult.success("续上一瓶红牛")
    }
}

object ElectricBatonEffectHandler : PropEffectHandler {
    override val codes: Set<String> = setOf(FunctionProps.ELECTRIC_BATON)

    override suspend fun use(prop: BaseProp, event: UseEvent): UseResult {
        val functionProp = prop as? FunctionProps
            ?: return UseResult.fail("该道具数据异常!")

        if (functionProp.electricity < 5) {
            return UseResult.fail("电棍没电了")
        }

        functionProp.electricity -= 5
        return UseResult.success("使用了电棍，电量剩余 ${functionProp.electricity}%")
    }
}

object MuteCardEffectHandler : PropEffectHandler {
    override val codes: Set<String> = setOf(FunctionProps.MUTE_1, FunctionProps.MUTE_30)

    override suspend fun use(prop: BaseProp, event: UseEvent): UseResult {
        val functionProp = prop as? FunctionProps
            ?: return UseResult.fail("该道具数据异常!")
        val subject = event.subject ?: return UseResult.fail("使用失败!")

        if (subject.botPermission == MemberPermission.MEMBER) {
            return UseResult.fail("使用失败!")
        }
        if (EconomyConfig.unableToUseMuteGroup.contains(subject.id)) {
            return UseResult.fail("该群禁言功能被禁用")
        }

        subject.sendMessage("请输入你想要禁言的人")
        val messageEvent = MessageUtil.INSTANCE.nextUserForGroupMessageEventSync(subject.id, event.sender.id, 180)
            ?: return UseResult.fail("使用失败!")
        val member = ShareUtils.getAtMember(messageEvent) ?: return UseResult.fail("使用失败!")

        member.mute(functionProp.muteTime * 60)
        return UseResult.success("禁言卡使用成功！")
    }
}
