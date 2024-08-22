package cn.chahuyun.economy.utils;

import cn.chahuyun.economy.HuYanEconomy;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.MessageEvent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 公共工具
 *
 * @author Moyuyanli
 * @date 2022/12/8 16:23
 */
public class ShareUtils {

    private ShareUtils() {
    }

    /**
     * 获取用户的下一次消息事件
     *
     * @param user    用户
     * @param subject 载体
     * @return MessageEvent or null
     * @author Moyuyanli
     * @date 2022/8/20 12:37
     */
    public static MessageEvent getNextMessageEventFromUser(User user, Contact subject) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<MessageEvent> result = new AtomicReference<>();
        GlobalEventChannel.INSTANCE.parentScope(HuYanEconomy.INSTANCE)
                .filterIsInstance(MessageEvent.class)
                .filter(filter -> filter.getSubject().getId() == subject.getId() && filter.getSender().getId() == user.getId())
                .subscribeOnce(MessageEvent.class, event -> {
                    result.set(event);
                    latch.countDown();
                });
        try {
            if (latch.await(10, TimeUnit.MINUTES)) {
                return result.get();
            } else {
                Log.debug("获取用户下一条消息超时");
                return null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("获取用户下一条消息失败");
        }
    }

    /**
     * 替换字符串中的变量
     *
     * @param s 原始字符串
     * @param variable 变量数组
     * @param content 替换内容数组
     * @return 替换后的字符串
     */
    public static String replacer(String s, String[] variable, Object... content) {
        for (int i = 0; i < variable.length; i++) {
            s = s.replace(variable[i], content[i].toString());
        }
        return s;
    }


}
