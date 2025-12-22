package cn.chahuyun.economy.fish;

import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.fish.Fish;
import cn.chahuyun.economy.entity.fish.FishInfo;
import cn.chahuyun.economy.entity.fish.FishPond;
import cn.chahuyun.economy.model.fish.FishBait;
import lombok.Getter;
import lombok.Setter;
import net.mamoe.mirai.event.AbstractEvent;

/**
 * roll鱼阶段
 *
 * @author Moyuyanli
 * @date 2024-11-14 16:58
 */
@Getter
@Setter
public class FishRollEvent extends AbstractEvent {

    /**
     * 用户信息
     */
    private final UserInfo userInfo;
    /**
     * 钓鱼信息
     */
    private final FishInfo fishInfo;
    /**
     * 鱼塘
     */
    private final FishPond fishPond;
    /**
     * 鱼饵
     */
    private final FishBait fishBait;

    /**
     * 实际差值
     */
    private final Float between;

    /**
     * 进化因子
     */
    private final Float evolution;

    /**
     * 最大鱼等级
     */
    private Integer maxGrade;
    /**
     * 最小鱼等级
     */
    private Integer minGrade;
    /**
     * 最大难度
     */
    private Integer maxDifficulty;
    /**
     * 最小难度
     */
    private Integer minDifficulty;
    /**
     * 惊喜
     */
    private boolean surprise;
    /**
     * 结果鱼
     */
    private Fish fish;

    public FishRollEvent(UserInfo userInfo, FishInfo fishInfo, FishPond fishPond, FishBait fishBait, Float between, Float evolution, Integer maxGrade, Integer minGrade, Integer maxDifficulty, Integer minDifficulty, boolean surprise) {
        this.userInfo = userInfo;
        this.fishInfo = fishInfo;
        this.fishPond = fishPond;
        this.fishBait = fishBait;
        this.between = between;
        this.evolution = evolution;
        this.maxGrade = maxGrade;
        this.minGrade = minGrade;
        this.maxDifficulty = maxDifficulty;
        this.minDifficulty = minDifficulty;
        this.surprise = surprise;
    }
}
