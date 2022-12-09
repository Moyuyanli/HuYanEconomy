package cn.chahuyun.economy.entity.fish;

import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.util.EconomyUtil;
import cn.chahuyun.economy.util.HibernateUtil;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.SingleMessage;

import java.io.Serializable;

/**
 * 钓鱼信息-玩家<p>
 *
 * @author Moyuyanli
 * @date 2022/12/8 10:48
 */
@Table
@Entity
@Getter
@Setter
public class FishInfo implements Serializable {

    @Id
    private long id;
    /**
     * 所属玩家
     */
    private long qq;
    /**
     * 是否购买鱼竿
     */
    private boolean fishRod;
    /**
     * 鱼竿等级
     */
    private int rodLevel;
    /**
     * 默认鱼塘
     */
    private String defaultFishPond;

    public FishInfo() {
    }

    /**
     * 创建钓鱼玩家<p>
     *
     * @param qq    玩家qq
     * @param group 默认鱼塘
     */
    public FishInfo(long qq, long group) {
        this.id = qq;
        this.qq = qq;
        this.fishRod = false;
        this.rodLevel = 0;
        this.defaultFishPond = "g." + group;
    }

    /**
     * 保存
     */
    public FishInfo save() {
        return HibernateUtil.factory.fromTransaction(session -> session.merge(this));
    }

    /**
     * 升级鱼竿<p>
     *
     * @param
     * @return net.mamoe.mirai.message.data.MessageChain
     * @author Moyuyanli
     * @date 2022/12/8 10:59
     */
    public SingleMessage updateRod(UserInfo userInfo) {
        if (getRodLevel() == 0) {
            double moneyByUser = EconomyUtil.getMoneyByUser(userInfo.getUser());
            if (moneyByUser - 1 < 0) {
                return new PlainText("你的金币不够拉！");
            }
        }
        return null;
    }

    /**
     * 获取默认鱼塘<p>
     *
     * @author Moyuyanli
     * @date 2022/12/8 15:11
     */
    public FishPond getFishPond() {
        try {
            return HibernateUtil.factory.fromSession(session -> session.get(FishPond.class, this.getDefaultFishPond()));
        } catch (Exception e) {
            String[] split = this.getDefaultFishPond().split("\\.");
            if (split.length == 2) {
                long group = Long.parseLong(split[1]);
                Group botGroup = HuYanEconomy.bot.getGroup(group);
                assert botGroup != null;
                FishPond fishPond = new FishPond(1, group, HuYanEconomy.config.getOwner(), botGroup.getName() + "鱼塘", "一个天然形成的鱼塘，无人管理，鱼情良好，深受钓鱼佬喜爱！");
                return HibernateUtil.factory.fromTransaction(session -> session.merge(fishPond));
            } else {
                return null;
            }
        }
    }

    /**
     * 获取钓鱼的鱼竿支持最大等级
     *
     * @return 鱼竿支持最大等级
     */
    public int getLevel() {
        return (int) (Math.ceil(getRodLevel() / 10.0));
    }


}
