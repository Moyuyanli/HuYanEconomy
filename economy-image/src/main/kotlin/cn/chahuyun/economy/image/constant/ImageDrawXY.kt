package cn.chahuyun.economy.image.constant

/**
 * 图片绘制位置
 */
enum class ImageDrawXY(val x: Int, val y: Int) {
    /** 头像绘制坐标。 */
    AVATAR(60, 70),

    /** QQ 号绘制坐标。 */
    ID(230, 100),

    /** 称号绘制坐标。 */
    TITLE(230, 135),

    /** 昵称绘制坐标。 */
    NICK_NAME(230, 200),

    /** 签到时间绘制坐标。 */
    SIGN_TIME(180, 340),

    /** 连续签到次数绘制坐标。 */
    SIGN_NUM(180, 385),

    /** 一言正文绘制坐标。 */
    A_WORD(556, 315),

    /** 一言作者/出处绘制坐标。 */
    A_WORD_FAMOUS(556, 410),

    /** 用户现金金币绘制坐标。 */
    MY_MONEY(118, 530),

    /** 本次签到收益绘制坐标。 */
    SIGN_OBTAIN(358, 530),

    /** 银行存款绘制坐标。 */
    BANK_MONEY(585, 530),

    /** 银行利息绘制坐标。 */
    BANK_INTEREST(858, 530)
}
