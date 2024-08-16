package cn.chahuyun.economy.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object RobMsgConfig : AutoSavePluginConfig("RobMsgConfig") {
    @ValueDescription("抢夺冷却时长(秒)")
    var robCoolTime: Long by value(120L)

    @ValueDescription("入狱被禁时长(秒)")
    var jailCoolTime: Long by value(3600L)

    @ValueDescription("抢夺受害者最大金钱数量")
    var robMaxMoney : Int by value(1000)

    @ValueDescription("抢夺成功消息")
    var robSuccessMsg: List<String> by value(listOf(
        "你抢到了 \${对象} \${金币} 枚金币"
    ))

    @ValueDescription("抢夺失败消息")
    var robFailMsg: List<String> by value(listOf(
        "你在抢 \${对象} 的金币时被发现了!"
    ))

    @ValueDescription("抢夺赔钱消息")
    var loseMoneyMsg: List<String> by value(listOf(
        "你在抢 \${对象} 的金币时被抓住了!因此赔了 \${金币} 元"
    ))

    @ValueDescription("被抓入狱消息")
    var robJailMsg: List<String> by value(listOf(
        "你在抢 \${对象} 的金币时被警察抓到了!你被关进了监狱，并罚款 \${金币} 元!CD延长至: \${时间}"
    ))

    @ValueDescription("金币不足提示")
    var robNotEnoughMsg: String by value("你没钱了！赔不起捏")
}