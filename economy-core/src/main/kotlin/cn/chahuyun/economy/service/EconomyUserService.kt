package cn.chahuyun.economy.service

import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.contact.User
import xyz.cssxsh.mirai.economy.service.EconomyAccount

/**
 * Core-facing user profile operations for feature modules.
 */
object EconomyUserService {

    @JvmStatic
    fun getOrCreate(user: User): UserInfoDto =
        UserCoreManager.getUserInfo(user)

    @JvmStatic
    fun get(account: EconomyAccount): UserInfoDto =
        UserCoreManager.getUserInfo(account)

    @JvmStatic
    fun findByQq(userId: Long?): UserInfoDto? =
        UserCoreManager.getUserInfo(userId)

    @JvmStatic
    fun findByFundingUuid(uuid: String?): UserInfoDto? =
        UserCoreManager.getUserInfo(uuid)
}
