package cn.chahuyun.manager;

import cn.chahuyun.constant.Constant;
import cn.chahuyun.entity.PropsCard;
import cn.chahuyun.entity.UserInfo;
import cn.chahuyun.plugin.PluginManager;
import cn.chahuyun.util.EconomyUtil;
import cn.hutool.core.util.RandomUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageUtils;
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
     * 签到
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

        MessageChain messages = MessageUtils.newChain(new QuoteReply(message));
        if (!userInfo.sign()) {
            messages.add(new PlainText("你今天已经签到过了哦!"));
            subject.sendMessage(messages);
        }

        double goldNumber;

        int randomNumber = RandomUtil.randomNumber();
        if (randomNumber > 7) {
            randomNumber = RandomUtil.randomNumber();
            if (randomNumber > 7) {
                goldNumber = RandomUtil.randomInt(200, 500);
            } else {
                goldNumber = RandomUtil.randomInt(100, 200);
            }
        } else {
            goldNumber = RandomUtil.randomInt(50, 100);
        }

        PropsManager propsManager = PluginManager.getPropsManager();

        List<PropsCard> cardS = (List<PropsCard>) propsManager.getPropsByUserFromCode(userInfo, Constant.SIGN_DOUBLE_SINGLE_CARD, PropsCard.class);

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


    }


}
