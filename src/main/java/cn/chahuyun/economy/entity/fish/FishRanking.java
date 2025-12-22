package cn.chahuyun.economy.entity.fish;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.SingleMessage;

import java.io.Serializable;
import java.util.Date;

/**
 * 钓鱼排行
 *
 * @author Moyuyanli
 * @date 2022/12/14 15:08
 */
@Entity(name = "FishRanking")
@Table
@Getter
@Setter
public class FishRanking implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    /**
     * 钓起着qq
     */
    private long qq;
    /**
     * 名称
     */
    private String name;
    /**
     * 尺寸
     */
    private int dimensions;
    /**
     * 金额
     */
    private double money;
    /**
     * 鱼竿等级
     */
    private int fishRodLevel;
    /**
     * 钓起来的时间
     */
    private Date date;
    /**
     * 钓起来的鱼
     */
    @ManyToOne(targetEntity = Fish.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "FishId")
    private Fish fish;
    /**
     * 钓起来的鱼塘
     */
    @ManyToOne(targetEntity = FishPond.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "FishPondId")
    private FishPond fishPond;

    public FishRanking() {
    }

    public FishRanking(long qq, String name, int dimensions, double money, int fishRodLevel, Fish fish, FishPond fishPond) {
        this.qq = qq;
        this.name = name;
        this.dimensions = dimensions;
        this.money = money;
        this.fishRodLevel = fishRodLevel;
        this.fish = fish;
        this.fishPond = fishPond;
        this.date = new Date();
    }


    /**
     * 显示排行榜信息
     *
     * @param top 名次
     * @return 消息
     * @author Moyuyanli
     * @date 2022/12/14 16:01
     */
    public SingleMessage getInfo(int top) {
        String message =
                "top:" + (top + 1) + "\n";
        if (top == 0 || top == 1 || top == 2) {
            String s = DateUtil.formatBetween(DateUtil.between(new Date(), getDate(), DateUnit.MS), BetweenFormatter.Level.MINUTE);
            message += "霸榜时间:" + s + "\n";
        }
        message +=
                "用户:" + getName() + "(鱼竿等级:" + getFishRodLevel() + ")\n" +
                        "尺寸:" + getDimensions() + "\n" +
                        "金额:" + getMoney() + "\n" +
                        "鱼:" + getFish().getName() + "(等级:" + getFish().getLevel() + ")\n" +
                        "鱼塘:" + getFishPond().getName() + "(鱼塘等级:" + getFishPond().getPondLevel() + ")";
        return new PlainText(message);
    }

    public int getId() {
        return id;
    }

    public FishRanking setId(int id) {
        this.id = id;
        return this;
    }

    public Fish getFish() {
        return fish;
    }

    public FishRanking setFish(Fish fish) {
        this.fish = fish;
        return this;
    }

    public FishPond getFishPond() {
        return fishPond;
    }

    public FishRanking setFishPond(FishPond fishPond) {
        this.fishPond = fishPond;
        return this;
    }
}
