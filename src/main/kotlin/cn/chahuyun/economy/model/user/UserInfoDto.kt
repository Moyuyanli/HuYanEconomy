package cn.chahuyun.economy.model.user

import cn.chahuyun.economy.HuYanEconomy.config
import cn.chahuyun.economy.model.fish.FishInfoDto
import cn.chahuyun.economy.proxy.EntityProxyRegistry
import cn.chahuyun.economy.utils.Log
import cn.hutool.core.date.CalendarUtil
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import java.util.*

/**
 * 用户信息DTO
 *
 * 业务层唯一使用的用户数据对象。
 * 屏蔽V1/V2实体差异，业务层只依赖此DTO。
 */
@Serializable
data class UserInfoDto(
    /** 用户ID（实体主键） */
    var id: String = "",
    /** QQ号 */
    var qq: Long = 0,
    /** 昵称 */
    var name: String = "",
    /** 注册群号 */
    var registerGroup: Long = 0,
    /** 注册时间 */
    var registerTime: Long = 0,
    /** 是否已签到 */
    var sign: Boolean = false,
    /** 上次签到时间 */
    var signTime: Long = 0,
    /** 连续签到天数 */
    var signNumber: Int = 0,
    /** 历史连续签到天数 */
    var oldSignNumber: Int = 0,
    /** 签到累计收益 */
    var signEarnings: Double = 0.0,
    /** 银行利息收益 */
    var bankEarnings: Double = 0.0,
    /** 默认私人银行编码 */
    var defaultPrivateBankCode: String = "",
    /** 资助UUID */
    var funding: String = "",
    /** 背包物品数量 */
    var backpackCount: Int = 0,
    /** 背包条目 */
    var backpacks: List<UserBackpackDto> = emptyList()
) {
    @Transient
    lateinit var user: User

    @Transient
    var group: Group? = null

    fun sign(): Boolean {
        if (signTime == 0L) {
            sign = true
            signTime = Date().time
            signNumber = 1
            return true
        }

        val calendar = CalendarUtil.calendar(DateUtil.offsetDay(Date(signTime), 1))
        calendar.set(java.util.Calendar.HOUR_OF_DAY, config.reSignTime)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        val time = calendar.time
        val between = DateUtil.between(time, Date(), DateUnit.MINUTE, false)
        Log.debug("账户:($qq)签到时差->$between")
        if (between < 0) {
            return false
        } else if (between <= 1440) {
            signNumber += 1
            if (signNumber == 2) {
                oldSignNumber = 0
            }
        } else {
            oldSignNumber = signNumber
            signNumber = 1
        }
        sign = true
        signTime = Date().time
        return true
    }

    fun getFishInfo(): FishInfoDto {
        val proxy = EntityProxyRegistry.get<FishInfoDto>("fish_info") ?: error("钓鱼信息代理器未初始化")
        return proxy.findById(qq) ?: proxy.save(
            FishInfoDto(
                id = qq,
                qq = qq,
                defaultFishPond = "g-$registerGroup"
            )
        )
    }

    fun getString(): String {
        return "用户名称:$name\n用户qq:$qq\n连续签到:${signNumber}天\n"
    }

    fun getProp(code: String): UserBackpackDto {
        return backpacks.find { it.propCode == code }
            ?: error("获取用户的第一个对应code道具错误:道具code不存在!")
    }

    fun getPropOrNull(code: String): UserBackpackDto? {
        return backpacks.find { it.propCode == code }
    }
}
