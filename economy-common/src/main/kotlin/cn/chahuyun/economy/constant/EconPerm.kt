package cn.chahuyun.economy.constant

/**
 * 权限相关常量
 */
object EconPerm {
    /** 钓鱼玩法权限码。 */
    const val FISH_PERM: String = "fish"

    /** 猜签/彩票玩法权限码。 */
    const val LOTTERY_PERM: String = "lottery"

    /** 抢劫玩法权限码。 */
    const val ROB_PERM: String = "rob"

    /** 红包玩法权限码。 */
    const val RED_PACKET_PERM: String = "red-pack"

    /** 签到黑名单权限码。 */
    const val SIGN_BLACK_PERM: String = "sign-black"

    /** 抽奖玩法权限码。 */
    const val RAFFLE_PERM: String = "raffle"

    /** 农场玩法权限码。 */
    const val FARM_PERM: String = "farm"

    object GROUP {
        /** 钓鱼玩法权限组显示名。 */
        const val FISH_PERM_GROUP: String = "钓鱼组"

        /** 猜签/彩票玩法权限组显示名。 */
        const val LOTTERY_PERM_GROUP: String = "猜签组"

        /** 抢劫玩法权限组显示名。 */
        const val ROB_PERM_GROUP: String = "抢劫组"

        /** 红包玩法权限组显示名。 */
        const val RED_PACKET_PERM_GROUP: String = "红包组"

        /** 禁止签到用户所在权限组显示名。 */
        const val SIGN_BLACK_GROUP: String = "签到黑名单"

        /** 抽奖玩法权限组显示名。 */
        const val RAFFLE_PERM_GROUP: String = "抽奖组"

        /** 农场玩法权限组显示名。 */
        const val FARM_PERM_GROUP: String = "农场组"
    }
}
