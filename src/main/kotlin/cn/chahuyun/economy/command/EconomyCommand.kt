package cn.chahuyun.economy.command

import cn.chahuyun.economy.BuildConstants
import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.repair.RepairManager
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand

class EconomyCommand : CompositeCommand(
    HuYanEconomy, "hye",
    description = "HuYanEconomy Command"
) {

    @SubCommand("v")
    @Description("查询当前壶言经济版本")
    suspend fun CommandSender.version() {
        sendMessage("当前壶言经济版本 ${BuildConstants.VERSION}")
    }

    @SubCommand("repair")
    @Description("修复版本迭代带来的错误")
    suspend fun CommandSender.repair() {
        sendMessage(RepairManager.init())
    }

}