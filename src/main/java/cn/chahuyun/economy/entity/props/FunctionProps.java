package cn.chahuyun.economy.entity.props;

import cn.chahuyun.economy.config.EconomyConfig;
import cn.chahuyun.economy.entity.UserFactor;
import cn.chahuyun.economy.exception.Operation;
import cn.chahuyun.economy.plugin.FactorManager;
import cn.chahuyun.economy.prop.PropBase;
import cn.chahuyun.economy.utils.MessageUtil;
import cn.chahuyun.economy.utils.ShareUtils;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.event.events.GroupMessageEvent;

import java.util.Date;

/**
 * 功能性道具
 *
 * @author Moyuyanli
 * @date 2024-10-15 11:36
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class FunctionProps extends PropBase {

    /**
     * 便携电棍
     */
    public final static String ELECTRIC_BATON = "baton";

    /**
     * 红牛
     */
    public final static String RED_EYES = "red-eyes";

    /**
     * 1分钟禁言卡
     */
    public final static String MUTE_1 = "mute-1";

    /**
     * 30分钟禁言卡
     */
    public final static String MUTE_30 = "mute-30";

    /**
     * 生效时间
     */
    private Date enableTime;

    /**
     * 电量(剩余使用次数)
     */
    private Integer electricity;

    /**
     * 禁言时间(分钟)
     */
    private Integer muteTime;

    /**
     * 商店显示描述
     *
     * @return 商店显示结果
     */
    @Override
    public String toShopInfo() {
        return "道具名称:" + this.getName() +
                "\n价格:" + this.getCost() + "金币" +
                "\n描述:" + this.getDescription();
    }

    @Override
    public String toString() {
        switch (super.getCode()) {
            case ELECTRIC_BATON:
                return "道具名称:" + this.getName() +
                        "\n价格:" + this.getCost() + "金币" +
                        "\n剩余电量:" + this.getElectricity() + "%" +
                        "\n描述:" + this.getDescription();
            default:
                return "道具名称:" + this.getName() +
                        "\n价格:" + this.getCost() + "金币" +
                        "\n描述:" + this.getDescription();
        }
    }

    /**
     * 使用该道具
     *
     * @param info 使用信息
     */
    @Override
    public void use(UseEvent info) {
        switch (this.getCode()) {
            case RED_EYES:
                UserFactor factor = FactorManager.getUserFactor(info.getUserInfo());
                String buff = factor.getBuffValue(RED_EYES);
                if (buff == null) {
                    factor.setBuffValue(RED_EYES, DateUtil.now());
                    FactorManager.merge(factor);
                    throw new Operation("你猛猛炫了一瓶红牛!", true);
                } else {
                    DateTime parse = DateUtil.parse(buff);
                    long between = DateUtil.between(new Date(), parse, DateUnit.MINUTE);
                    if (between > 10) {
                        factor.setBuffValue(RED_EYES, DateUtil.now());
                        FactorManager.merge(factor);
                        throw new Operation("续上一瓶红牛!", true);
                    } else {
                        throw new Operation("红牛喝多了可对肾不好!");
                    }
                }
            case ELECTRIC_BATON:
                if (electricity >= 5) {
                    electricity -= 5;
                } else {
                    throw new RuntimeException("电棒没电了!");
                }
                break;
            case MUTE_1:
            case MUTE_30:
                Contact subject = info.getSubject();
                if (subject instanceof Group) {
                    Group group = (Group) subject;
                    if (group.getBotPermission() != MemberPermission.MEMBER) {
                        if (EconomyConfig.INSTANCE.getUnableToUseMuteGroup().contains(subject.getId())) {
                            throw new Operation("该群禁言功能被禁用!");
                        }

                        subject.sendMessage("请输入你想要禁言的人");
                        GroupMessageEvent messageEvent = MessageUtil.INSTANCE.nextUserForGroupMessageEventSync(subject.getId(),info.getSender().getId(),180);
                        if (messageEvent != null) {
                            Member member = ShareUtils.getAtMember(messageEvent);
                            if (member != null) {
                                member.mute(muteTime * 60);
                                throw new Operation("禁言卡使用成功！", true);
                            }
                        }
                    }

                }
                throw new Operation("使用失败!");
            default:
                throw new Operation("该道具无法直接使用!");
        }
    }
}
