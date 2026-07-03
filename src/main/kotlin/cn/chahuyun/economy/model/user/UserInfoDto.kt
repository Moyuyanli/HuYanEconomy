package cn.chahuyun.economy.model.user

import cn.chahuyun.economy.HuYanEconomy.config
import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.fish.FishInfoDto
import cn.chahuyun.economy.utils.Log
import cn.hutool.core.date.CalendarUtil
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import kotlinx.serialization.Serializable
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import java.util.*

/**
 * 鐢ㄦ埛淇℃伅DTO
 *
 * 涓氬姟灞傚敮涓€浣跨敤鐨勭敤鎴锋暟鎹璞°€? * 灞忚斀V1/V2瀹炰綋宸紓锛屼笟鍔″眰鍙緷璧栨DTO銆? */
@Serializable
data class UserInfoDto(
    /** 鐢ㄦ埛ID锛堝疄浣撲富閿級 */
    var id: String = "",
    /** QQ鍙?*/
    var qq: Long = 0,
    /** 鏄电О */
    var name: String = "",
    /** 娉ㄥ唽缇ゅ彿 */
    var registerGroup: Long = 0,
    /** 娉ㄥ唽鏃堕棿 */
    var registerTime: Long = 0,
    /** 鏄惁宸茬鍒?*/
    var sign: Boolean = false,
    /** 涓婃绛惧埌鏃堕棿 */
    var signTime: Long = 0,
    /** 杩炵画绛惧埌澶╂暟 */
    var signNumber: Int = 0,
    /** 鍘嗗彶杩炵画绛惧埌澶╂暟 */
    var oldSignNumber: Int = 0,
    /** 绛惧埌绱鏀剁泭 */
    var signEarnings: Double = 0.0,
    /** 閾惰鍒╂伅鏀剁泭 */
    var bankEarnings: Double = 0.0,
    /** 榛樿绉佷汉閾惰缂栫爜 */
    var defaultPrivateBankCode: String = "",
    /** 璧勫姪UUID */
    var funding: String = "",
    /** 鑳屽寘鐗╁搧鏁伴噺 */
    var backpackCount: Int = 0,
    /** 鑳屽寘鏉＄洰 */
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
        Log.debug("璐︽埛:($qq)绛惧埌鏃跺樊->$between")
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
        return "鐢ㄦ埛鍚嶇О:$name\n鐢ㄦ埛qq:$qq\n杩炵画绛惧埌:${signNumber}澶‐n"
    }

    fun getProp(code: String): UserBackpackDto {
        return backpacks.find { it.propCode == code }
            ?: error("鑾峰彇鐢ㄦ埛鐨勭涓€涓搴攃ode閬撳叿閿欒:閬撳叿code涓嶅瓨鍦?")
    }

    fun getPropOrNull(code: String): UserBackpackDto? {
        return backpacks.find { it.propCode == code }
    }
}
