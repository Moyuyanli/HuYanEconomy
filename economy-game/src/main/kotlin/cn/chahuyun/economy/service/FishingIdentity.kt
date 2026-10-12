package cn.chahuyun.economy.service

import cn.chahuyun.economy.data.repository.NewUserInfoRepository
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.model.user.group

/**
 * 钓鱼用户身份适配。
 *
 * 2.0 新实体规范要求业务表 `user_id` 引用 `hye_user_info.id`，而不是 QQ 号。
 * 钓鱼模块统一通过此处把运行时用户转换为稳定的内部主键，避免在业务代码中散落解析逻辑。
 */
object FishingIdentity {

    /** 解析并返回 `hye_user_info.id`。 */
    fun userId(userInfo: UserInfoDto): Long =
        NewUserInfoRepository.getOrCreateId(
            qq = userInfo.qq,
            name = userInfo.name,
            registerGroup = userInfo.group?.id?.toString() ?: "",
        )
}
