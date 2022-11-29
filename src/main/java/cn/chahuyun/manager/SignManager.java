package cn.chahuyun.manager;

import cn.chahuyun.constant.Constant;
import cn.chahuyun.entity.UserInfo;
import cn.chahuyun.entity.props.PropsCard;
import cn.chahuyun.plugin.PluginManager;
import cn.chahuyun.util.EconomyUtil;
import cn.hutool.core.util.RandomUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.QuoteReply;

import java.util.List;

/**
 * 签到管理<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:25
 */
public class SignManager {

    private SignManager() {

    }


    /**
     * 签到<p>
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/11/15 14:53
     */
    public static void sign(MessageEvent event) {
        User user = event.getSender();
        Contact subject = event.getSubject();
        MessageChain message = event.getMessage();

        UserInfo userInfo = UserManager.getUserInfo(user);

        MessageChainBuilder messages = new MessageChainBuilder();
        messages.append(new QuoteReply(message));
        if (userInfo == null) {
            subject.sendMessage("签到失败!");
            return;
        }
        if (!userInfo.sign()) {
            messages.append(new PlainText("你今天已经签到过了哦!"));
            subject.sendMessage(messages.build());
            return;
        }

        double goldNumber;

        PlainText plainText = null;

        int randomNumber = RandomUtil.randomInt(0, 10);
        if (randomNumber > 7) {
            randomNumber = RandomUtil.randomInt(0, 10);
            if (randomNumber > 8) {
                goldNumber = RandomUtil.randomInt(200, 500);
                plainText = new PlainText(String.format("\n卧槽,你家祖坟裂了,冒出%s金币", goldNumber));
            } else {
                goldNumber = RandomUtil.randomInt(100, 200);
                plainText = new PlainText(String.format("\n哇偶,你今天运气爆棚,获得%s金币", goldNumber));
            }
        } else {
            goldNumber = RandomUtil.randomInt(50, 100);
        }

        PropsManager propsManager = PluginManager.getPropsManager();

        List<PropsCard> cardS = propsManager.getPropsByUserFromCode(userInfo, Constant.SIGN_DOUBLE_SINGLE_CARD, PropsCard.class);

        boolean doubleStatus = false;
        for (PropsCard card : cardS) {
            if (card.isStatus()) {
                doubleStatus = true;
                if (!propsManager.deleteProp(userInfo, card, PropsCard.class)) {
                    doubleStatus = false;
                    subject.sendMessage("双倍签到金币卡使用失败!");
                }
                break;
            }
        }

        if (doubleStatus) {
            goldNumber = goldNumber * 2;
        }

        if (!EconomyUtil.addMoneyToUser(user, goldNumber)) {
            subject.sendMessage("签到失败!");
            //todo 签到失败回滚
            return;
        }

        double moneyByUser = EconomyUtil.getMoneyByUser(user);

        messages.append(new PlainText("签到成功!\n"));
        messages.append(new PlainText(userInfo.getString()));
        messages.append(new PlainText(String.format("金币:%s(+%s)", moneyByUser, goldNumber)));
        if (userInfo.getOldSignNumber() != 0) {
            messages.append(String.format("你的连签线断在了%d天,可惜~", userInfo.getOldSignNumber()));
        }
        if (plainText != null) {
            messages.append(plainText);
        }
        subject.sendMessage(messages.build());
    }


}
