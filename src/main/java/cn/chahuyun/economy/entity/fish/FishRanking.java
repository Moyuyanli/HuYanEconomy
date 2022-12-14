package cn.chahuyun.economy.entity.fish;

import cn.chahuyun.economy.util.HibernateUtil;
import jakarta.persistence.*;
import lombok.Data;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.SingleMessage;

import java.io.Serializable;

/**
 * 钓鱼排行
 *
 * @author Moyuyanli
 * @date 2022/12/14 15:08
 */
@Data
@Entity(name = "FishRanking")
@Table
public class FishRanking implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    /**
     * 吊起着qq
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
     * 钓起来的鱼
     */
    @ManyToOne(targetEntity = Fish.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "fish_id")
    private Fish fish;
    /**
     * 钓起来的鱼塘
     */
    @ManyToOne(targetEntity = FishPond.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "fish_pond_id")
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
    }

    /**
     * 保存
     */
    public FishRanking save() {
        return HibernateUtil.factory.fromTransaction(session -> session.merge(this));
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
                "top:" + top+1 + "\n" +
                        "用户:" + getName() + "(鱼竿等级:" + getFishRodLevel() + ")\n" +
                        "尺寸:" + getDimensions() + "\n" +
                        "金额:" + getMoney() + "\n" +
                        "鱼:" + getFish().getName() + "(等级:" + getFish().getLevel() + ")\n" +
                        "鱼塘:" + getFishPond().getName() + "(鱼塘等级:" + getFishPond().getPondLevel() + ")";
        return new PlainText(message);
    }


}
