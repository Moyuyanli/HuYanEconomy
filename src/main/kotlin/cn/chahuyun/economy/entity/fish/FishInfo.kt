@file:Suppress("unused")

package cn.chahuyun.economy.entity.fish

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.hibernateplus.HibernateFactory
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.SingleMessage
import java.io.Serializable

/**
 * 钓鱼信息-玩家
 *
 * @author Moyuyanli
 * @date 2022/12/8 10:48
 */
@Table
@Entity(name = "FishInfo")
class FishInfo(
    @Id
    var id: Long = 0,

    /**
     * 所属玩家
     */
    var qq: Long = 0,

    /**
     * 是否购买鱼竿
     */
    var isFishRod: Boolean = false,

    /**
     * 是否在钓鱼
     */
    var status: Boolean = false,

    /**
     * 鱼竿等级
     */
    var rodLevel: Int = 0,

    /**
     * 默认鱼塘
     */
    var defaultFishPond: String? = null
) : Serializable {

    /**
     * 创建钓鱼玩家
     *
     * @param qq 玩家qq
     * @param group 默认鱼塘
     */
    constructor(qq: Long, group: Long) : this(
        id = qq,
        qq = qq,
        isFishRod = false,
        status = false,
        rodLevel = 0,
        defaultFishPond = "g-$group"
    )

    /**
     * 升级鱼竿
     *
     * @param userInfo 用户信息
     * @return net.mamoe.mirai.message.data.MessageChain
     */
    fun updateRod(userInfo: UserInfo): SingleMessage {
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

    /**
     * 获取默认鱼塘
     *
     * @see FishPond
     */
    fun getFishPond(): FishPond? {
        var fishPond: FishPond?
        try {
            val map = HashMap<String, String>()
            map["code"] = this.defaultFishPond ?: ""
            fishPond = HibernateFactory.selectOne(FishPond::class.java, map)
            if (fishPond != null) return fishPond
        } catch (e: Exception) {
            Log.debug(e)
        }

        val split = this.defaultFishPond?.split("-") ?: return null
        if (split.size == 2) {
            val group = split[1].toLong()
            val botGroup = HuYanEconomy.bot?.getGroup(group)
            val finalFishPond = if (botGroup != null) {
                FishPond(1, group, HuYanEconomy.config.owner, botGroup.name + "鱼塘", "一个天然形成的鱼塘，无人管理，鱼情良好，深受钓鱼佬喜爱！")
            } else {
                FishPond(1, 0, 0, "空鱼塘", "一个天然形成的鱼塘，无人管理，鱼情良好，深受钓鱼佬喜爱！")
            }
            return HibernateFactory.merge(finalFishPond)
        }
        return null
    }

    /**
     * 获取群鱼塘
     *
     * @see FishPond
     */
    fun getFishPond(group: Group): FishPond {
        var fishPond = HibernateFactory.selectOne(FishPond::class.java, "code", "g-${group.id}")
        if (fishPond != null) return fishPond

        fishPond = FishPond(1, group.id, HuYanEconomy.config.owner, group.name + "鱼塘", "一个天然形成的鱼塘，无人管理，鱼情良好，深受钓鱼佬喜爱！")
        return HibernateFactory.merge(fishPond)
    }

    /**
     * 获取钓鱼的鱼竿支持最大等级
     */
    val level: Int
        get() = if (rodLevel == 0) 1 else rodLevel / 10 + 2

    /**
     * 鱼竿等级+1
     */
    private fun upFishRod() {
        this.rodLevel += 1
        HibernateFactory.merge(this)
    }

    /**
     * 相同的升级
     *
     * @param user 用户
     * @param userMoney 用户拥有的金币
     * @param upMoney 升级鱼竿的金币
     * @return 成功消息
     */
    private fun isMoney(user: User, userMoney: Double, upMoney: Int): SingleMessage {
        if (userMoney - upMoney < 0) {
            return PlainText(String.format("你的金币不够%s啦！", upMoney))
        }
        if (EconomyUtil.minusMoneyToUser(user, upMoney.toDouble())) {
            upFishRod()
            return PlainText(String.format("升级成功,花费%s金币!你的鱼竿更强了!\n%s->%s", upMoney, this.rodLevel - 1, rodLevel))
        }
        return PlainText("升级失败!")
    }

    /**
     * 线程安全获取钓鱼状态
     *
     * @return true 在钓鱼
     */
    @get:Synchronized
    val isStatus: Boolean
        get() {
            return if (status) {
                true
            } else {
                status = true
                HibernateFactory.merge(this)
                false
            }
        }


    /**
     * 关闭钓鱼状态
     */
    fun switchStatus() {
        this.status = false
        HibernateFactory.merge(this)
    }
}
