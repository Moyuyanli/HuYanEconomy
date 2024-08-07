package cn.chahuyun.economy.constant;

import lombok.Getter;

/**
 * 图片绘制位置
 *
 * @author Moyuyanli
 * @Date 2024/8/7 22:23
 */
@Getter
public enum ImageDrawXY {

    /**
     * 头像
     */
    AVATAR(60, 70),
    /**
     * QQ号
     */
    ID(230, 120),
    /**
     * 名称
     */
    NICK_NAME(230, 180),
    /**
     * 签到时间
     */
    SIGN_TIME(180, 340),
    /**
     * 签到次数
     */
    SIGN_NUM(180, 385),
    /**
     * 一言正文
     */
    A_WORD(556, 315),
    /**
     * 一言署名
     */
    A_WORD_FAMOUS(556, 410),
    /**
     * 我的金币
     */
    MY_MONEY(118, 530),
    /**
     * 签到获得
     */
    SIGN_OBTAIN(358, 530),
    /**
     * 银行存款
     */
    BANK_MONEY(580, 530),
    /**
     * 银行利息
     */
    BANK_INTEREST(858, 530);


    private final int x;
    private final int y;

    ImageDrawXY(int x, int y) {
        this.x = x;
        this.y = y;
    }
}