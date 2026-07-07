package cn.chahuyun.economy.service

import cn.chahuyun.economy.model.user.UserInfoDto

/**
 * Formats user profile snippets used by legacy text fallbacks.
 */
object EconomyUserProfileFormatter {

    @JvmStatic
    fun basicInfo(userInfo: UserInfoDto): String {
        return "用户名称:${userInfo.name}\n用户qq:${userInfo.qq}\n连续签到:${userInfo.signNumber}天\n"
    }
}
