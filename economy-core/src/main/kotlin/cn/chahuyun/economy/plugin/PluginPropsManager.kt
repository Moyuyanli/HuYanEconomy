package cn.chahuyun.economy.plugin

import cn.chahuyun.economy.constant.PropsKind
import cn.chahuyun.economy.model.props.FunctionProps
import cn.chahuyun.economy.model.props.PropsCard
import cn.chahuyun.economy.prop.Expirable
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.prop.effect.*
import cn.chahuyun.economy.scheduler.HuYanScheduler
import cn.chahuyun.economy.utils.Log

/**
 * 插件道具管理 (Kotlin 重构版)
 */
object PluginPropsManager {

    @JvmStatic
    fun init() {
        // 注册道具类型
        PropsManager.registerKindToPropClass(PropsKind.card, PropsCard::class.java)
        PropsManager.registerKindToPropClass(PropsKind.functionProp, FunctionProps::class.java)
        PropEffectRegistry.registerAll(
            RedEyesEffectHandler,
            ElectricBatonEffectHandler,
            MuteCardEffectHandler,
            NameChangeCardEffectHandler,
            CardActivationEffectHandler
        )

        // 初始化签到卡
        initCards()
        // 初始化功能道具
        initFunctionProps()

        // 每天凌晨4点检查过期道具
        HuYanScheduler.schedule("prop-expire-check", "0 0 4 * * ?", PropExpireCheckTask())
    }

    private fun initCards() {
        val cards = listOf(
            PropsCard(PropsKind.card, PropsCard.SIGN_2, "签到双倍金币卡").apply {
                description = "\"不要999，不要888，只要88金币，你的下一次签到将翻倍！\""
                cost = 88
                canBuy = true
                canItExpire = false
                isConsumption = true
            },
            PropsCard(PropsKind.card, PropsCard.SIGN_3, "签到三倍金币卡").apply {
                description = "\"不要999，不要888，只要188金币，你的下一次签到将翻三倍！\""
                cost = 188
                canBuy = true
                canItExpire = false
                isConsumption = true
            },
            PropsCard(PropsKind.card, PropsCard.HEALTH, "医保卡").apply {
                description = "少年，你还在为付不起医药费而发愁吗？？？"
                cost = 5888
                canBuy = true
                canItExpire = false
                isStack = false
            },
            PropsCard(PropsKind.card, PropsCard.NAME_CHANGE, "改名卡").apply {
                description = "刷新你现在的信息!"
                cost = 2333
                canBuy = true
                canItExpire = false
                isConsumption = true
            },
            PropsCard(PropsKind.card, PropsCard.SIGN_IN, "补签卡").apply {
                description = "\"花123购买一张补签卡，将会在你的下次签到自动生效\""
                cost = 123
                canBuy = true
                canItExpire = false
                status = true
                isConsumption = true
            },
            PropsCard(PropsKind.card, PropsCard.MONTHLY, "签到月卡").apply {
                description = "持续一个月的5倍经济，无法与签到卡同时生效!"
                cost = 9999
                canBuy = true
                status = true
                isStack = false
                canItExpire = true
                expireDays = 30
            }
        )
        cards.forEach { PropsManager.registerCodeToProp(it) }
    }

    private fun initFunctionProps() {
        val props = listOf(
            FunctionProps(PropsKind.functionProp, FunctionProps.ELECTRIC_BATON, "便携电棍").apply {
                description = "用于防身,或许有其他用途?"
                cost = 1888
                canBuy = true
                electricity = 100
                unit = "把"
                isStack = false
            },
            FunctionProps(PropsKind.functionProp, FunctionProps.RED_EYES, "红牛").apply {
                description = "喝完就有劲了!"
                cost = 888
                canBuy = true
                unit = "瓶"
                isConsumption = true
            },
            FunctionProps(PropsKind.functionProp, FunctionProps.MUTE_1, "1分钟禁言卡").apply {
                description = "你怎么不说话了？"
                cost = 3600
                canBuy = true
                muteTime = 1
                unit = "张"
                isConsumption = true
            },
            FunctionProps(PropsKind.functionProp, FunctionProps.MUTE_30, "30分钟禁言卡").apply {
                description = "对人宝具:强制退网半小时！"
                cost = 90000
                canBuy = true
                muteTime = 30
                unit = "张"
                isConsumption = true
            },
            FunctionProps(PropsKind.functionProp, FunctionProps.FARM_RAFFLE_BASIC, "初级农场抽奖券").apply {
                description = "农场系统的初级抽奖凭证，后续抽奖系统开放后可使用。"
                canBuy = false
                unit = "张"
                isStack = true
                isConsumption = false
            },
            FunctionProps(PropsKind.functionProp, FunctionProps.FARM_RAFFLE_ADVANCED, "高级农场抽奖券").apply {
                description = "农场系统的高级抽奖凭证，后续抽奖系统开放后可使用。"
                canBuy = false
                unit = "张"
                isStack = true
                isConsumption = false
            }
        )
        props.forEach { PropsManager.registerCodeToProp(it) }
    }
}

class PropExpireCheckTask : Runnable {
    override fun run() {
        val collect = PropsManager.listPropsData()
        for (data in collect) {
            try {
                val kind = data.kind.takeIf { it.isNotBlank() } ?: continue
                val id = data.id.takeIf { it != 0L } ?: continue
                val propClass = PropsManager.getPropClass(kind) ?: continue
                val prop = PropsManager.deserialization(data, propClass)

                if (prop is Expirable && prop.isExpired()) {
                    Log.info("道具已过期，正在销毁: kind=$kind, code=${prop.code}, id=${id}")
                    PropsManager.destroyProsAndBackpack(id)
                }
            } catch (e: Exception) {
                Log.error("检查道具过期时发生异常，道具 id=${data.id}", e)
            }
        }
    }
}
