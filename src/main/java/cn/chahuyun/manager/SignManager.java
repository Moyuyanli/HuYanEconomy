package cn.chahuyun.manager;

import cn.chahuyun.entity.UserInfo;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.QuoteReply;

/**
 * 签到管理<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:25
 */
public class SignManager {

    private SignManager() {

    }

    public static void sign(MessageEvent event) {
        User user = event.getSender();
        UserInfo userInfo = UserManager.getUserInfo(user);
        if (userInfo.sign()) {
            MessageChainBuilder singleMessages = new MessageChainBuilder();
            QuoteReply quoteReply = new QuoteReply(event.getMessage());
            singleMessages.append(quoteReply)
                    .append(userInfo.getString());
            event.getSubject().sendMessage(singleMessages.build());
        }
    }


}
