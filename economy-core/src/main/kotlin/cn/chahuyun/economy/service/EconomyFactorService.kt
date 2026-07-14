package cn.chahuyun.economy.service

import cn.chahuyun.economy.model.GlobalFactorDto
import cn.chahuyun.economy.model.user.UserFactorDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.plugin.FactorManager
import cn.hutool.core.date.DateUtil
import java.util.*

/**
 * Core-facing factor operations for feature modules.
 */
object EconomyFactorService {

    @JvmStatic
    fun globalFactor(): GlobalFactorDto =
        FactorManager.getGlobalFactor()

    @JvmStatic
    fun userFactor(userInfo: UserInfoDto): UserFactorDto =
        FactorManager.getUserFactor(userInfo)

    @JvmStatic
    fun saveUserFactor(factor: UserFactorDto) =
        FactorManager.merge(factor)

    @JvmStatic
    fun getUserBuff(userInfo: UserInfoDto, buffName: String): String? =
        UserFactorBuffCodec.getBuffValue(userFactor(userInfo), buffName)

    @JvmStatic
    fun setUserBuff(userInfo: UserInfoDto, buffName: String, value: String) =
        saveUserFactor(UserFactorBuffCodec.withBuffValue(userFactor(userInfo), buffName, value))

    @JvmStatic
    fun clearUserBuff(userInfo: UserInfoDto, buffName: String) =
        saveUserFactor(UserFactorBuffCodec.withBuffValue(userFactor(userInfo), buffName, null))

    @JvmStatic
    fun setUserBuffStartedNow(userInfo: UserInfoDto, buffName: String, now: Date = Date()) =
        setUserBuff(userInfo, buffName, now.time.toString())

    @JvmStatic
    fun getUserBuffStartedAt(userInfo: UserInfoDto, buffName: String): Date? {
        val value = getUserBuff(userInfo, buffName) ?: return null
        return value.toLongOrNull()?.let(::Date)
            ?: runCatching { DateUtil.parse(value) }.getOrNull()
    }

    @JvmStatic
    fun isUserBuffActive(
        userInfo: UserInfoDto,
        buffName: String,
        durationMinutes: Int,
        now: Date = Date(),
    ): Boolean {
        val startedAt = getUserBuffStartedAt(userInfo, buffName) ?: return false
        val elapsed = now.time - startedAt.time
        return elapsed >= 0 && elapsed < durationMinutes * 60_000L
    }
}
