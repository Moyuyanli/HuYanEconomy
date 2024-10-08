package cn.chahuyun.economy.sign;

import cn.chahuyun.economy.entity.UserInfo;
import lombok.Getter;
import lombok.Setter;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.AbstractEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.QuoteReply;

/**
 * 签到事件管理
 *
 * @author Moyuyanli
 * @date 2024/9/25 16:58
 */
@Getter
@Setter
public class SignEvent extends AbstractEvent {

    /**
     * 签到人
     */
    private final UserInfo userInfo;
    /**
     * 随机参数<br>
     * 0~500: 50~100<br>
     * 501~850: 101~200<br>
     * 851~999: 201~500<br>
     * 1000: 999
     */
    private Integer param;
    /**
     * 文字消息
     */
    private MessageChain reply;
    /**
     * 事件回复消息
     */
    private MessageChainBuilder eventReply;
    /**
     * 双倍签到
     */
    private boolean sign_2 = false;
    /**
     * 三倍签到
     */
    private boolean sign_3 = false;
    /**
     * 补签
     */
    private boolean sign_in = false;
    /**
     * 群
     */
    private final Group group;
    /**
     * 签到消息
     */
    private final MessageChain messages;
    /**
     * 消息事件
     */
    private final GroupMessageEvent event;
    /**
     * 签到金额
     */
    private Double gold;


    public SignEvent(UserInfo userInfo, GroupMessageEvent event) {
        this.userInfo = userInfo;
        this.event = event;
        this.group = event.getGroup();
        this.messages = event.getMessage();
    }

    /**
     * 添加事件消息回复
     *
     * @param messages 事件消息
     */
    public void eventReplyAdd(MessageChain messages) {
        if (this.eventReply == null) {
            this.eventReply = new MessageChainBuilder();
            this.eventReply.add(messages);
        } else {
            this.eventReply.add(new PlainText("\n"));
            this.eventReply.add(messages);
        }

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
