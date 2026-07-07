package cn.chahuyun.economy.service

import cn.chahuyun.economy.utils.MoneyFormatUtil
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.SingleMessage

object FishingRodMessageFormatter {

    fun maxLevel(): SingleMessage =
        PlainText("你的鱼竿已经满级了！")

    fun coinNotEnough(upgradeCost: Int): SingleMessage =
        PlainText("你的金币不够${MoneyFormatUtil.format(upgradeCost.toDouble())}哦！")

    fun upgradeSuccess(upgradeCost: Int, oldLevel: Int, newLevel: Int): SingleMessage =
        PlainText("升级成功,花费${MoneyFormatUtil.format(upgradeCost.toDouble())}金币!你的鱼竿更强了!\n$oldLevel->$newLevel")

    fun upgradeFailed(): SingleMessage =
        PlainText("升级失败!")

    fun currentLevel(rodLevel: Int): String =
        "你的鱼竿等级为${rodLevel}级"
}
