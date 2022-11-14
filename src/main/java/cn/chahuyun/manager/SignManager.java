package cn.chahuyun.manager;

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

    }


}
