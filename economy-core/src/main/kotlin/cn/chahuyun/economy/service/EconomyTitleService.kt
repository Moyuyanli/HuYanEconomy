package cn.chahuyun.economy.service

import cn.chahuyun.economy.manager.TitleManager
import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.contact.Contact

/**
 * Core-facing title operations for feature modules.
 */
object EconomyTitleService {

    @JvmStatic
    fun isEnabled(userInfo: UserInfoDto, titleCode: String): Boolean =
        TitleManager.checkTitleIsOnEnable(userInfo, titleCode)

    @JvmStatic
    fun exists(userInfo: UserInfoDto, titleCode: String): Boolean =
        TitleManager.checkTitleIsExist(userInfo, titleCode)

    @JvmStatic
    fun grant(userInfo: UserInfoDto, titleCode: String): Boolean =
        TitleManager.addTitleInfo(userInfo, titleCode)

    @JvmStatic
    suspend fun checkFishTitle(userInfo: UserInfoDto, subject: Contact) =
        TitleManager.checkFishTitle(userInfo, subject)
}
