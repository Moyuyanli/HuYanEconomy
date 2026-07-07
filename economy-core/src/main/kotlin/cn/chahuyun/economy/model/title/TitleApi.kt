package cn.chahuyun.economy.model.title

import cn.chahuyun.economy.model.user.TitleInfoDto
import cn.chahuyun.economy.model.user.UserInfoDto

/**
 * 称号 API 接口
 */
interface TitleApi {
    /**
     * 创建称号信息
     *
     * @param userInfo 用户信息
     * @return 称号信息
     */
    fun createTitleInfo(userInfo: UserInfoDto): TitleInfoDto
}
