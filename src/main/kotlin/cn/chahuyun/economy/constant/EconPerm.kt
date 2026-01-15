package cn.chahuyun.economy.constant

/**
 * 权限相关常量
 */
object EconPerm {
    const val FISH_PERM: String = "fish"
    const val LOTTERY_PERM: String = "lottery"
    const val ROB_PERM: String = "rob"
    const val RED_PACKET_PERM: String = "red-pack"
    const val SIGN_BLACK_PERM: String = "sign-black"
    const val RAFFLE_PERM: String = "raffle"

    object GROUP {
        const val FISH_PERM_GROUP: String = "钓鱼组"
        const val LOTTERY_PERM_GROUP: String = "猜签组"
        const val ROB_PERM_GROUP: String = "抢劫组"
        const val RED_PACKET_PERM_GROUP: String = "红包组"
        const val SIGN_BLACK_GROUP: String = "签到黑名单"
        const val RAFFLE_PERM_GROUP: String = "抽奖组"
    }
}
