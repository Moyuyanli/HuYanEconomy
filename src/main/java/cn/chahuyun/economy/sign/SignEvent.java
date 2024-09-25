package cn.chahuyun.economy.sign;

import cn.chahuyun.economy.entity.UserInfo;
import lombok.Getter;
import lombok.Setter;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.AbstractEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.QuoteReply;

/**
 * 签到事件管理
 *
 * @author Moyuyanli
 * @date 2024/9/25 16:58
 */
public class SignEvent extends AbstractEvent {

    /**
     * 签到人
     */
    private final UserInfo userInfo;

    /**
     * 群
     */
    @Getter
    private final Group group;

    /**
     * 消息
     */
    @Getter
    private final MessageChain messages;

    /**
     * 消息事件
     */
    @Getter
    private final GroupMessageEvent event;

    /**
     * 签到金额
     */
    @Getter
    @Setter
    private Double gold;


    public SignEvent(UserInfo userInfo, GroupMessageEvent event) {
        this.userInfo = userInfo;
        this.event = event;
        this.group = event.getGroup();
        this.messages = event.getMessage();
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    /**
     * 引用回复
     *
     * @param messages 回复的消息
     * @return 返回的消息
     */
    public MessageReceipt<Group> sendQuote(MessageChain messages) {
        return group.sendMessage(new QuoteReply(this.messages).plus(messages));
    }

}
