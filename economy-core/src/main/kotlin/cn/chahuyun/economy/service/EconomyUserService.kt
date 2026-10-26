package cn.chahuyun.economy.service

import cn.chahuyun.economy.feature.user.UserManager
import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.contact.User

/**
 * Core-facing user profile operations for feature modules.
 */
object EconomyUserService {

    @JvmStatic
    fun getOrCreate(user: User): UserInfoDto =
        UserManager.getUserInfo(user)

    /** 按经济账户 ID（`u<qq>`）反查用户。 */
    @JvmStatic
    fun getByAccountId(accountId: String): UserInfoDto? =
        UserManager.getUserInfoByAccountId(accountId)

    @JvmStatic
    fun findByQq(userId: Long?): UserInfoDto? =
        UserManager.getUserInfo(userId)

    @JvmStatic
    fun findByFundingUuid(uuid: String?): UserInfoDto? =
        UserManager.getUserInfo(uuid)
}
