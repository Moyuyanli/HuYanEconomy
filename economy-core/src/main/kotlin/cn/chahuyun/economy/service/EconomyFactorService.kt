package cn.chahuyun.economy.service

import cn.chahuyun.economy.model.GlobalFactorDto
import cn.chahuyun.economy.model.user.UserFactorDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.plugin.FactorManager

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
}
