package cn.chahuyun.economy.constant

/**
 * 称号 code
 */
object TitleCode {
    /** 连续签到 15 天称号。 */
    const val SIGN_15: String = "sign-15"

    /** 连续签到 15 天称号有效期：15 天。 */
    const val SIGN_15_EXPIRED: Int = 15

    /** 连续签到 90 天称号。 */
    const val SIGN_90: String = "sign-90"

    /** 连续签到 90 天称号有效期：365 天。 */
    const val SIGN_90_EXPIRED: Int = 365

    /** 金币超过 100000 的大富翁称号。 */
    const val MONOPOLY: String = "monopoly-0"

    /** -1 表示永久有效。 */
    const val MONOPOLY_EXPIRED: Int = -1

    /** 金币超过 10000 的小富翁称号。 */
    const val REGAL: String = "regal-30"

    /** 小富翁称号有效期：30 天。 */
    const val REGAL_EXPIRED: Int = 30

    /** 钓鱼排行榜榜首称号。 */
    const val FISHING: String = "fishing"

    /** -1 表示永久有效。 */
    const val FISHING_EXPIRED: Int = -1

    /** 赌怪称号。 */
    const val BET_MONSTER: String = "bet-monster"

    /** -1 表示永久有效。 */
    const val BET_MONSTER_EXPIRED: Int = -1

    /** 抢劫玩法称号。 */
    const val ROB: String = "rob"

    /** -1 表示永久有效。 */
    const val ROB_EXPIRED: Int = -1
}
