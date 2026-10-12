package cn.chahuyun.economy.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

/**
 * 钓鱼玩法消息配置。
 *
 * 所有面向用户的可配置文案集中在此，业务代码不得硬编码这些提示。
 */
object FishingMsgConfig : AutoSavePluginConfig("Fishing") {

    @ValueDescription("购买鱼竿价格")
    val buyRodPrice: Int by value(500)

    @ValueDescription("已拥有鱼竿时的购买提示")
    val repeatPurchaseRod: String by value("你已经拥有鱼竿了哦，不用再买啦!")

    @ValueDescription("金币不足购买鱼竿提示")
    val coinNotEnoughForRod: String by value("购买鱼竿需要 500 金币，你的金币不足!")

    @ValueDescription("购买鱼竿成功提示")
    val buyFishingRodSuccess: String by value("购买鱼竿成功，快去钓鱼吧!")

    @ValueDescription("没有鱼竿时升级提示")
    val noneRodUpgradeMsg: String by value("你还没有鱼竿，先购买鱼竿吧!")

    @ValueDescription("钓鱼过程中尝试升级鱼竿提示")
    val upgradeWhenFishing: String by value("钓鱼的时候就不要升级鱼竿了!")

    @ValueDescription("金币不足升级鱼竿提示")
    val coinNotEnoughForRodUpgrade: String by value("你的金币不足以升级鱼竿!")

    @ValueDescription("鱼竿已满级提示")
    val rodMaxLevel: String by value("你的鱼竿已经满级了!")

    @ValueDescription("升级鱼竿成功提示")
    val upgradeRodSuccess: String by value("鱼竿升级成功，当前等级:%s!")

    @ValueDescription("金币不足升级鱼塘提示")
    val coinNotEnoughForPond: String by value("你的金币不足以升级鱼塘!")

    @ValueDescription("鱼竿等级不足升级鱼塘提示")
    val rodLevelNotEnoughForPond: String by value("你的鱼竿等级不足以升级鱼塘!")

    @ValueDescription("鱼塘已满级提示")
    val pondMaxLevel: String by value("你的鱼塘已经满级了!")

    @ValueDescription("升级鱼塘成功提示")
    val upgradePondSuccess: String by value("鱼塘升级成功，当前等级:%s!")

    @ValueDescription("鱼塘名称已被占用提示")
    val pondNameDuplicated: String by value("你已经有一个同名鱼塘了，换一个名字吧!")

    @ValueDescription("创建鱼塘成功提示")
    val createPondSuccess: String by value("鱼塘创建成功:%s!")
}
