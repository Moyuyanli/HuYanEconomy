package cn.chahuyun.economy.service

import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.service.FishingRuntimeService.activeSessions
import cn.chahuyun.economy.service.FishingRuntimeService.playerCooling
import cn.chahuyun.economy.utils.MessageUtil
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.contact.Contact
import java.util.concurrent.ConcurrentHashMap

/**
 * 钓鱼运行时状态。
 *
 * 冷却与并发会话状态都是短生命周期数据，不落库：
 * - [playerCooling] 记录上次抛竿时间，用于计算冷却；
 * - [activeSessions] 防止同一用户并发钓鱼。
 */
object FishingRuntimeService {

    private val playerCooling = ConcurrentHashMap<Long, Long>()
    private val activeSessions = ConcurrentHashMap.newKeySet<Long>()

    // ---- 冷却 ----

    /** 冷却分钟数：持有钓鱼达人称号固定 5 分钟，否则随鱼竿等级下降。 */
    fun cooldownMinutes(rodLevel: Int, isFishingTitle: Boolean): Int =
        if (isFishingTitle) 5 else ((10 * 60 - rodLevel * 3) / 60).coerceAtLeast(1)

    fun remainingCooldownMillis(
        qq: Long,
        rodLevel: Int,
        isFishingTitle: Boolean,
        now: Long = System.currentTimeMillis(),
    ): Long {
        val last = playerCooling[qq] ?: return 0L
        val cooldown = cooldownMinutes(rodLevel, isFishingTitle).toLong() * 60_000L
        return (last + cooldown - now).coerceAtLeast(0L)
    }

    fun markFishing(qq: Long) {
        playerCooling[qq] = System.currentTimeMillis()
    }

    fun removeCooling(qq: Long) {
        playerCooling.remove(qq)
    }

    fun clearCooling() {
        playerCooling.clear()
    }

    // ---- 并发会话 ----

    fun tryBeginSession(qq: Long): Boolean = activeSessions.add(qq)

    fun endSession(qq: Long) {
        activeSessions.remove(qq)
    }

    fun clearSessions() {
        activeSessions.clear()
    }

    /** 该用户是否正在钓鱼。 */
    fun isFishing(qq: Long): Boolean = activeSessions.contains(qq)

    /** 刷新全部钓鱼状态（清空冷却与进行中的会话）。 */
    fun resetAll() {
        clearCooling()
        clearSessions()
    }

    fun minutesRoundedUp(durationMillis: Long): Long =
        (durationMillis.coerceAtLeast(0L) + 59_999L) / 60_000L

    /**
     * 钓鱼失败判定。
     *
     * 极小概率触发“钓到尸体”特殊事件（报警入狱），一定概率提示空军。
     *
     * @return true 表示本次钓鱼已中断
     */
    suspend fun failedFishing(userInfo: UserInfoDto, subject: Contact): Boolean {
        val errorMessages = arrayOf("风吹的...", "眼花了...", "走神了...", "呀！切线了...", "钓鱼佬绝不空军！")
        val randomed = RandomUtil.randomInt(0, 10001)
        return when {
            randomed >= 9998 -> {
                subject.sendMessage(
                    MessageUtil.formatMessageChain(userInfo.qq, "你钓起来一具尸体，附近的钓鱼佬报警了，你真是百口莫辩啊！")
                )
                EconomyUserStatusService.movePrison(userInfo, 60)
                true
            }

            randomed >= 6000 -> {
                subject.sendMessage(MessageUtil.formatMessageChain(userInfo.qq, errorMessages[RandomUtil.randomInt(0, 5)]))
                true
            }

            else -> false
        }
    }
}
