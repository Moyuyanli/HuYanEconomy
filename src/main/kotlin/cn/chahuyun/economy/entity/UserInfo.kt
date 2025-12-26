package cn.chahuyun.economy.entity

import cn.chahuyun.economy.HuYanEconomy.config
import cn.chahuyun.economy.entity.fish.FishInfo
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.date.CalendarUtil
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import jakarta.persistence.*
import net.mamoe.mirai.console.permission.AbstractPermitteeId
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import java.io.Serializable
import java.util.*

/**
 * 用户信息
 *
 * @author Moyuyanli
 * @date 2022/11/14 9:45
 */
@Entity(name = "UserInfo")
@Table(name = "UserInfo")
class UserInfo(
    @Id
    var id: String? = null,

    /**
     * qq号
     */
    var qq: Long = 0,

    /**
     * 名称
     */
    var name: String? = null,

    /**
     * 注册群号
     */
    var registerGroup: Long = 0,

    /**
     * 注册时间
     */
    var registerTime: Date? = null,

    /**
     * 签到状态
     */
    var sign: Boolean = false,

    /**
     * 签到时间
     */
    var signTime: Date? = null,

    /**
     * 连续签到次数
     */
    var signNumber: Int = 0,

    /**
     * 断掉的连续签到次数
     */
    var oldSignNumber: Int = 0,

    /**
     * 签到收益
     */
    var signEarnings: Double = 0.0,

    /**
     * 银行收益
     */
    var bankEarnings: Double = 0.0,

    /**
     * 绑定key
     */
    var funding: String? = null,

    /**
     * 道具背包
     */
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "UserInfo_id")
    var backpacks: MutableList<UserBackpack> = mutableListOf()
) : Serializable {

    @Transient
    lateinit var user: User

    @Transient
    var group: Group? = null

    constructor(qq: Long, registerGroup: Long, name: String?, registerTime: Date?) : this(
        id = AbstractPermitteeId.ExactUser(qq).asString(),
        qq = qq,
        registerGroup = registerGroup,
        name = name,
        registerTime = registerTime
    )

    /**
     * 签到
     *
     * @return boolean true 签到成功 false 签到失败
     */
    fun sign(): Boolean {
        // 如果签到时间为空 -> 新用户 第一次签到
        if (this.signTime == null) {
            this.sign = true
            this.signTime = Date()
            this.signNumber = 1
            HibernateFactory.merge(this)
            return true
        }
        // 获取签到时间，向后偏移一天
        val calendar = CalendarUtil.calendar(DateUtil.offsetDay(signTime, 1))
        // 自定义更新签到时间 默认为 04:00:00
        calendar.set(Calendar.HOUR_OF_DAY, config.reSignTime)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val time = calendar.time
        // 获取小时数差
        val between = DateUtil.between(time, Date(), DateUnit.MINUTE, false)
        Log.debug("账户:(${this.qq})签到时差->$between")
        // 时间还在24小时之内 则为负数
        if (between < 0) {
            return false
        } else if (between <= 1440) {
            this.signNumber += 1
            if (this.signNumber == 2) {
                this.oldSignNumber = 0
            }
        } else {
            this.oldSignNumber = this.signNumber
            this.signNumber = 1
        }
        this.sign = true
        this.signTime = Date()
        HibernateFactory.merge(this)
        return true
    }

    /**
     * 将这个道具添加到用户背包
     *
     * @param userBackpack 背包格信息
     * @return boolean true 成功
     */
    fun addPropToBackpack(userBackpack: UserBackpack): Boolean {
        return try {
            this.backpacks.add(userBackpack)
            HibernateFactory.merge(this)
            true
        } catch (e: Exception) {
            Log.error("用户信息:添加道具到背包出错", e)
            false
        }
    }

    /**
     * 从背包删除一个道具
     *
     * @param userBackpack 背包道具
     * @return true 成功
     */
    fun removePropInBackpack(userBackpack: UserBackpack): Boolean {
        return this.backpacks.remove(userBackpack)
    }

    fun isSign(): Boolean {
        val now = DateUtil.format(Date(), "yyyy-MM-dd") + " 04:00:00"
        val nowDate = DateUtil.parse(now)
        val signDate = signTime ?: return false
        val between = DateUtil.between(nowDate, signDate, DateUnit.HOUR, false)
        return between > 0
    }

    /**
     * 获取钓鱼信息
     * 不存在则注册一个
     *
     * @return FishInfo 钓鱼信息
     */
    fun getFishInfo(): FishInfo {
        var fishInfo: FishInfo?
        try {
            fishInfo = HibernateFactory.selectOneById<FishInfo>(this.qq)
            if (fishInfo != null) return fishInfo
        } catch (_: Exception) {
        }
        val newFishInfo = FishInfo(this.qq, this.registerGroup)
        return HibernateFactory.merge(newFishInfo)
    }

    fun getString(): String {
        return "用户名称:${this.name}\n用户qq:${this.qq}\n连续签到:${this.signNumber}天\n"
    }

    /**
     * 获取第一个对应的道具
     *
     * @param code 道具code
     * @return 背包道具
     */
    fun getProp(code: String): UserBackpack {
        return backpacks.find { it.propCode == code } ?: error("获取用户的第一个对应code道具错误:道具code不存在!")
    }


    /**
     * 获取第一个对应的道具
     *
     * @param code 道具code
     * @return 背包道具
     */
    fun getPropOrNull(code: String): UserBackpack? {
        return backpacks.find { it.propCode == code }
    }

}
