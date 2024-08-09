package cn.chahuyun.economy.constant;

import lombok.Getter;

/**
 * @author Moyuyanli
 * @date 2024/8/9 10:34
 */
@Getter
public enum TitleTemplate {

    /**
     * 大富翁
     */
    MONOPOLY(-1),
    /**
     * 小富翁
     */
    LITTLE_RICH_MAN(15),
    /**
     * 签到狂人
     */
    SIGN_IN_MADMAN(15);

    /**
     * 有效期(天)
     */
    private final int validityPeriod;

    TitleTemplate(int validityPeriod) {
        this.validityPeriod = validityPeriod;
    }
}
