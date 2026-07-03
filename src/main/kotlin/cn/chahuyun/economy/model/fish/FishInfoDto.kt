package cn.chahuyun.economy.model.fish

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.MoneyFormatUtil
import kotlinx.serialization.Serializable
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.SingleMessage

/**
 * 鐢ㄦ埛閽撻奔淇℃伅DTO
 */
@Serializable
data class FishInfoDto(
    /** 璁板綍ID锛堢敤鎴稩D锛?*/
    var id: Long = 0,
    /** QQ鍙?*/
    var qq: Long = 0,
    /** 鏄惁鎷ユ湁楸肩 */
    var isFishRod: Boolean = false,
    /** 鏄惁姝ｅ湪閽撻奔 */
    var status: Boolean = false,
    /** 楸肩绛夌骇 */
    var rodLevel: Int = 0,
    /** 榛樿楸煎缂栫爜 */
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
                    name = "${group.name}楸煎",
                    description = "一个天然形成的鱼塘，鱼情良好。",
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
            else -> PlainText("浣犵殑楸肩宸茬粡婊＄骇鎷夛紒")
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
            return PlainText("浣犵殑閲戝竵涓嶅${MoneyFormatUtil.format(upMoney.toDouble())}鍟︼紒")
        }
        if (EconomyUtil.minusMoneyToUser(user, upMoney.toDouble())) {
            upFishRod()
            return PlainText("鍗囩骇鎴愬姛,鑺辫垂${MoneyFormatUtil.format(upMoney.toDouble())}閲戝竵!浣犵殑楸肩鏇村己浜?\n${rodLevel - 1}->${rodLevel}")
        }
        return PlainText("鍗囩骇澶辫触!")
    }

    private val fishInfoProxy
        get() = EntityProxyRegistry.get<FishInfoDto>("fish_info") ?: error("钓鱼信息代理器未初始化")

    private val fishPondProxy
        get() = EntityProxyRegistry.get<FishPondDto>("fish_pond") ?: error("鱼塘代理器未初始化")
}
