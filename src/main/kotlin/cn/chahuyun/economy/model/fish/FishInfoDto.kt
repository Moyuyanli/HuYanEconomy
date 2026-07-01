package cn.chahuyun.economy.model.fish

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.proxy.EntityProxyRegistry
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.MoneyFormatUtil
import kotlinx.serialization.Serializable
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.SingleMessage

/**
 * 用户钓鱼信息DTO
 */
@Serializable
data class FishInfoDto(
    /** 记录ID（用户ID） */
    var id: Long = 0,
    /** QQ号 */
    var qq: Long = 0,
    /** 是否拥有鱼竿 */
    var isFishRod: Boolean = false,
    /** 是否正在钓鱼 */
    var status: Boolean = false,
    /** 鱼竿等级 */
    var rodLevel: Int = 0,
    /** 默认鱼塘编码 */
    var defaultFishPond: String = ""
) {
    fun getFishPond(group: Group): FishPondDto {
        val code = "g-${group.id}"
        return fishPondProxy.findByKey(code)
            ?: fishPondProxy.save(
                FishPondDto(
                    code = code,
                    admin = HuYanEconomy.config.owner,
                    pondType = 1,
                    name = "${group.name}鱼塘",
                    description = "一个天然形成的鱼塘，无人管理，鱼情良好，深受钓鱼佬喜爱！",
                    pondLevel = 6
                )
            )
    }

    val level: Int
        get() = if (rodLevel == 0) 1 else rodLevel / 10 + 2

    fun updateRod(userInfo: UserInfoDto): SingleMessage {
        val user = userInfo.user
        val moneyByUser = EconomyUtil.getMoneyByUser(user)
        var upMoney = 1
        return when {
            rodLevel == 0 -> isMoney(user, moneyByUser, upMoney)
            rodLevel in 1..69 -> {
                upMoney = 40 * rodLevel * level
                isMoney(user, moneyByUser, upMoney)
            }
            rodLevel in 70..79 -> {
                upMoney = 80 * rodLevel * level
                isMoney(user, moneyByUser, upMoney)
            }
            rodLevel in 80..89 -> {
                upMoney = 100 * rodLevel * level
                isMoney(user, moneyByUser, upMoney)
            }
            rodLevel in 90..98 -> {
                upMoney = 150 * rodLevel * level
                isMoney(user, moneyByUser, upMoney)
            }
            rodLevel == 99 -> {
                upMoney = 150000
                isMoney(user, moneyByUser, upMoney)
            }
            else -> PlainText("你的鱼竿已经满级拉！")
        }
    }

    fun switchStatus() {
        status = false
        save()
    }

    fun save(): FishInfoDto = fishInfoProxy.save(this).also { saved ->
        id = saved.id
        qq = saved.qq
        isFishRod = saved.isFishRod
        status = saved.status
        rodLevel = saved.rodLevel
        defaultFishPond = saved.defaultFishPond
    }

    private fun upFishRod() {
        rodLevel += 1
        save()
    }

    private fun isMoney(user: User, userMoney: Double, upMoney: Int): SingleMessage {
        if (userMoney - upMoney < 0) {
            return PlainText("你的金币不够${MoneyFormatUtil.format(upMoney.toDouble())}啦！")
        }
        if (EconomyUtil.minusMoneyToUser(user, upMoney.toDouble())) {
            upFishRod()
            return PlainText("升级成功,花费${MoneyFormatUtil.format(upMoney.toDouble())}金币!你的鱼竿更强了!\n${rodLevel - 1}->${rodLevel}")
        }
        return PlainText("升级失败!")
    }

    private val fishInfoProxy
        get() = EntityProxyRegistry.get<FishInfoDto>("fish_info") ?: error("钓鱼信息代理器未初始化")

    private val fishPondProxy
        get() = EntityProxyRegistry.get<FishPondDto>("fish_pond") ?: error("鱼塘代理器未初始化")
}
