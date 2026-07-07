package cn.chahuyun.economy.prop.effect

import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.props.PropsCard
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.prop.BaseProp
import cn.chahuyun.economy.prop.UseResult
import net.mamoe.mirai.contact.nameCardOrNick

object NameChangeCardEffectHandler : PropEffectHandler {
    override val codes: Set<String> = setOf(PropsCard.NAME_CHANGE)

    override suspend fun use(prop: BaseProp, event: UseEvent): UseResult {
        val sender = event.sender
        val userInfo = UserCoreManager.getUserInfo(sender)
        userInfo.name = sender.nameCardOrNick
        UserCoreManager.saveUserInfo(userInfo)

        return UseResult.success("改名卡使用成功!")
    }
}

object CardActivationEffectHandler : PropEffectHandler {
    override val codes: Set<String> = setOf(
        PropsCard.SIGN_2,
        PropsCard.SIGN_3,
        PropsCard.SIGN_IN,
        PropsCard.MONTHLY,
        PropsCard.HEALTH
    )

    override suspend fun use(prop: BaseProp, event: UseEvent): UseResult {
        val card = prop as? PropsCard
            ?: return UseResult.fail("该道具数据异常!")

        card.status = true
        return UseResult.success("${card.name} 使用成功")
    }
}
