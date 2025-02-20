package cn.chahuyun.economy.entity.props;

import cn.chahuyun.economy.entity.UserInfo;
import lombok.Getter;
import lombok.Setter;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;

import java.util.Date;

/**
 *
 *
 * @author Moyuyanli
 * @date 2025-2-20 10:16
 */
@Getter
@Setter
public class UseEvent {

    /**
     * 发送着
     */
    private final User sender;
    /**
     * 发送群
     */
    private final Contact subject;
    /**
     * 用户信息
     */
    private final UserInfo userInfo;
    /**
     * 时间
     */
    private final Date time = new Date();

    public UseEvent(User sender, Contact subject, UserInfo userInfo) {
        this.sender = sender;
        this.subject = subject;
        this.userInfo = userInfo;
    }
}
