package cn.chahuyun.economy.fish;

import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.fish.FishBait;
import cn.chahuyun.economy.entity.fish.FishInfo;
import lombok.Getter;
import lombok.Setter;
import net.mamoe.mirai.event.AbstractEvent;

/**
 * 钓鱼开始事件
 *
 * @author Moyuyanli
 * @date 2024-11-14 10:15
 */
@Getter
@Setter
public class FishStartEvent extends AbstractEvent {
    /**
     * 用户信息
     */
    private final UserInfo userInfo;
    /**
     * 钓鱼信息
     */
    private final FishInfo fishInfo;

    /**
     * 鱼饵
     */
    private FishBait fishBait;
    /**
     * 最大鱼等级
     */
    private Integer maxGrade;
    /**
     * 最大难度
     */
    private Integer maxDifficulty;
    /**
     * 最小难度
     */
    private Integer minDifficulty;


    public FishStartEvent(UserInfo userInfo, FishInfo fishInfo) {
        this.userInfo = userInfo;
        this.fishInfo = fishInfo;
    }

    /**
     * 计算最大等级
     * @return 最大等级
     */
    protected Integer calculateMaxGrade() {
        return fishInfo.getLevel();
    }

    /**
     * 计算最大难度
     * @return 最大难度
     */
    protected Integer calculateMaxDifficulty() {
        return fishInfo.getRodLevel() * fishBait.getLevel();
    }

    /**
     * 计算基本难度
     * @return 基本难度
     */
    protected Integer calculateBaseDifficulty() {
        return Math.round(fishInfo.getRodLevel() * fishBait.getQuality());
    }
}
