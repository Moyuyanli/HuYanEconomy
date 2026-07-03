package cn.chahuyun.economy.manager

import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.user.UserRaffleDto
import java.util.concurrent.ConcurrentHashMap

/**
 * 鎶藉鐩稿叧鐨勨€滈潪浜嬩欢鐩戝惉鈥濋€昏緫銆?
 *
 * 璇存槑锛?
 * - `action.LuckyDrawAction` 浠呬繚鐣欐寚浠ゅ叆鍙ｄ笌娑堟伅鍙戦€併€?
 * - 鍐峰嵈銆佺敤鎴锋娊濂栫粺璁℃暟鎹殑鑾峰彇绛夊彲澶嶇敤閫昏緫涓嬫矇鍒拌繖閲屻€?
 */
object LuckyDrawManager {

    // 鍗曟娊鍐峰嵈鏃堕棿(60绉?
    private const val SINGLE_COOLDOWN = 60_000L

    // 鍗佽繛鍐峰嵈鏃堕棿(600绉?
    private const val TEN_COOLDOWN = 600_000L

    // 鐢ㄦ埛鍐峰嵈璁板綍 (userId -> 涓婃鎶藉鏃堕棿鎴?
    private val singleCooldownMap = ConcurrentHashMap<Long, Long>()
    private val tenCooldownMap = ConcurrentHashMap<Long, Long>()

    @JvmStatic
    fun take(userId: Long): UserRaffleDto {
        return userRaffleProxy.findById(userId) ?: userRaffleProxy.save(UserRaffleDto(id = userId))
    }

    @JvmStatic
    fun checkSingleCooldown(userId: Long): Boolean {
        val lastTime = singleCooldownMap[userId] ?: 0L
        return System.currentTimeMillis() - lastTime >= SINGLE_COOLDOWN
    }

    @JvmStatic
    fun checkTenCooldown(userId: Long): Boolean {
        val lastTime = tenCooldownMap[userId] ?: 0L
        return System.currentTimeMillis() - lastTime >= TEN_COOLDOWN
    }

    @JvmStatic
    fun singleRemainingSeconds(userId: Long): Int {
        val lastTime = singleCooldownMap[userId] ?: 0L
        val remainingMs = SINGLE_COOLDOWN - (System.currentTimeMillis() - lastTime)
        return (remainingMs.coerceAtLeast(0) / 1000).toInt()
    }

    @JvmStatic
    fun tenRemainingSeconds(userId: Long): Int {
        val lastTime = tenCooldownMap[userId] ?: 0L
        val remainingMs = TEN_COOLDOWN - (System.currentTimeMillis() - lastTime)
        return (remainingMs.coerceAtLeast(0) / 1000).toInt()
    }

    @JvmStatic
    fun updateCooldown(userId: Long, isTen: Boolean = false) {
        val now = System.currentTimeMillis()
        if (isTen) tenCooldownMap[userId] = now else singleCooldownMap[userId] = now
    }

    private val userRaffleProxy
        get() = EntityProxyRegistry.get<UserRaffleDto>("user_raffle") ?: error("用户抽奖代理器未初始化")
}


