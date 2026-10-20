package cn.chahuyun.economy.service

import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.contact.User

/**
 * Core-facing user profile operations for feature modules.
 */
object EconomyUserService {

    @JvmStatic
    fun getOrCreate(user: User): UserInfoDto =
        UserCoreManager.getUserInfo(user)

    /** 按经济账户 ID（`u<qq>`）反查用户。 */
    @JvmStatic
    fun getByAccountId(accountId: String): UserInfoDto? =
        UserCoreManager.getUserInfoByAccountId(accountId)

    @JvmStatic
    fun findByQq(userId: Long?): UserInfoDto? =
        UserCoreManager.getUserInfo(userId)

    @JvmStatic
    fun findByFundingUuid(uuid: String?): UserInfoDto? =
        UserCoreManager.getUserInfo(uuid)
}
