package cn.chahuyun.manager;

import cn.chahuyun.entity.UserInfo;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;

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
            event.getSubject().sendMessage("签到成功!");
        }
    }


}
