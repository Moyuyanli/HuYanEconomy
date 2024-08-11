package cn.chahuyun.economy.command

import cn.chahuyun.economy.HuYanEconomy
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand

class EconomyCommand : CompositeCommand(
    HuYanEconomy.INSTANCE, "hye",
    description = "HuYanEconomy Command"
) {

    @SubCommand("v")
    @Description("查询当前壶言经济版本")
    suspend fun CommandSender.version() {
        sendMessage("当前壶言经济版本 ${HuYanEconomy.VERSION}")
    }

}