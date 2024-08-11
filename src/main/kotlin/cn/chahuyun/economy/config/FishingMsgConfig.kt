package cn.chahuyun.economy.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object FishingMsgConfig : AutoSavePluginConfig("FishingMsgConfig") {
    @ValueDescription("没有鱼竿时钓鱼提示")
    var noneRodMsg : String by value("你还没有鱼竿，发送\"购买鱼竿\"购买吧")

    @ValueDescription("正在钓鱼时的提示")
    var fishingNowMsg : String by value("你已经在钓鱼了！")

    @ValueDescription("鱼竿等级不够提示")
    var rodLevelNotEnough : String by value("你的鱼竿太拉了，这里不让你来，升升级吧...")

    @ValueDescription("有鱼竿时购买鱼竿提示")
    var repeatPurchaseRod : String by value("你已经有一把钓鱼竿了，不用再买了！")

    @ValueDescription("购买鱼竿时金币不够提示")
    var coinNotEnoughForRod : String by value("我这把钓鱼竿可是神器！你的金币还远远不够！等攒够了500金币再来吧")

    @ValueDescription("购买鱼竿成功提示")
    var buyFishingRodSuccess : String by value("拿好了，这鱼竿到手即不负责，永不提供售后！")

    @ValueDescription("没有鱼竿时升级提示")
    var noneRodUpgradeMsg : String by value("鱼竿都没有，无法升级!")

    @ValueDescription("钓鱼期间升级提示")
    var upgradeWhenFishing : String by value("钓鱼期间不可升级鱼竿!")
}