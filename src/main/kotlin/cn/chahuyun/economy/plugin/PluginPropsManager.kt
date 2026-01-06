package cn.chahuyun.economy.plugin

import cn.chahuyun.economy.constant.PropsKind
import cn.chahuyun.economy.entity.props.PropsData
import cn.chahuyun.economy.model.fish.FishBait
import cn.chahuyun.economy.model.props.FunctionProps
import cn.chahuyun.economy.model.props.PropsCard
import cn.chahuyun.economy.prop.Expirable
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.cron.CronUtil
import cn.hutool.cron.task.Task

/**
 * 插件道具管理 (Kotlin 重构版)
 */
object PluginPropsManager {

    @JvmStatic
    fun init() {
        // 注册道具类型
        PropsManager.registerKindToPropClass(PropsKind.card, PropsCard::class.java)
        PropsManager.registerKindToPropClass(PropsKind.functionProp, FunctionProps::class.java)
        PropsManager.registerKindToPropClass(PropsKind.fishBait, FishBait::class.java)

        // 初始化签到卡
        initCards()
        // 初始化功能道具
        initFunctionProps()
        // 初始化鱼饵
        initFishBaits()

        // 每天凌晨4点检查过期道具
        CronUtil.schedule("0 0 4 * * ?", PropExpireCheckTask())
    }

    private fun initCards() {
        val cards = listOf(
            PropsCard(PropsKind.card, PropsCard.SIGN_2, "签到双倍金币卡").apply {
                description = "\"不要999，不要888，只要88金币，你的下一次签到将翻倍！\""
                cost = 88
                canBuy = true
                canItExpire = false
            },
            PropsCard(PropsKind.card, PropsCard.SIGN_3, "签到三倍金币卡").apply {
                description = "\"不要999，不要888，只要188金币，你的下一次签到将翻三倍！\""
                cost = 188
                canBuy = true
                canItExpire = false
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
            },
            PropsCard(PropsKind.card, PropsCard.SIGN_IN, "补签卡").apply {
                description = "\"花123购买一张补签卡，将会在你的下次签到自动生效\""
                cost = 123
                canBuy = true
                canItExpire = false
                status = true
            },
            PropsCard(PropsKind.card, PropsCard.MONTHLY, "签到月卡").apply {
                description = "持续一个月的5倍经济，无法与签到卡同时生效!"
                cost = 9999
                canBuy = true
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
                // 红牛是消耗品，逻辑由重构后的 ConsumableProp 处理（虽然目前 FunctionProps 还没继承它，但后续可以统一）
            },
            FunctionProps(PropsKind.functionProp, FunctionProps.MUTE_1, "1分钟禁言卡").apply {
                description = "你怎么不说话了？"
                cost = 3600
                canBuy = true
                muteTime = 1
                unit = "张"
            },
            FunctionProps(PropsKind.functionProp, FunctionProps.MUTE_30, "30分钟禁言卡").apply {
                description = "对人宝具:强制退网半小时！"
                cost = 90000
                canBuy = true
                muteTime = 30
                unit = "张"
            }
        )
        props.forEach { PropsManager.registerCodeToProp(it) }
    }

    private fun initFishBaits() {
        val baits = listOf(
            FishBait(PropsKind.fishBait, FishBait.BAIT_1, "基础鱼饵").apply {
                num = 25
                level = 1
                quality = 0.08f
                cost = 66
                description = "最基础的鱼饵，量大管饱(鱼管饱)"
                canBuy = true
                unit = "包"
            },
            FishBait(PropsKind.fishBait, FishBait.BAIT_2, "中级鱼饵").apply {
                num = 20
                level = 2
                quality = 0.15f
                cost = 269
                description = "中级鱼饵，闻着就有一股香味。"
                canBuy = true
                unit = "包"
            },
            FishBait(PropsKind.fishBait, FishBait.BAIT_3, "高级鱼饵").apply {
                num = 15
                level = 3
                quality = 0.25f
                cost = 588
                description = "除了贵，全是优点"
                canBuy = true
                unit = "包"
            },
            FishBait(PropsKind.fishBait, FishBait.BAIT_L_1, "特化型(香味)鱼饵").apply {
                num = 18
                level = 4
                quality = 0.05f
                cost = 450
                description = "袋子都封不住他的气味，看来传播性很好！"
                canBuy = true
                unit = "包"
            },
            FishBait(PropsKind.fishBait, FishBait.BAIT_Q_1, "特化型(口味)鱼饵").apply {
                num = 18
                level = 2
                quality = 0.30f
                cost = 350
                description = "我家鱼吃了都说好吃！"
                canBuy = true
                unit = "包"
            }
        )
        baits.forEach { PropsManager.registerCodeToProp(it) }
    }
}

class PropExpireCheckTask : Task {
    override fun execute() {
        val collect = HibernateFactory.selectList(PropsData::class.java)
        for (data in collect) {
            try {
                val kind = data.kind ?: continue
                val id = data.id ?: continue
                val propClass = PropsManager.shopClass(kind) ?: continue
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

