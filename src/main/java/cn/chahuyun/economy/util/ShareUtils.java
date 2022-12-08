package cn.chahuyun.economy.util;

import cn.chahuyun.economy.HuYanEconomy;
import kotlin.coroutines.EmptyCoroutineContext;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.ConcurrencyKind;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.EventPriority;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.MessageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 公共工具
 *
 * @author Moyuyanli
 * @date 2022/12/8 16:23
 */
public class ShareUtils {

    private ShareUtils(){}

    /**
     * 获取用户的下一次消息事件
     *
     * @param user 用户
     * @param type 是否拦截该用户的其他指令响应
     * @return MessageEvent
     * @author Moyuyanli
     * @date 2022/8/20 12:37
     */
    @NotNull
    public static MessageEvent getNextMessageEventFromUser(User user,boolean type) {
        EventChannel<MessageEvent> channel = GlobalEventChannel.INSTANCE.parentScope(HuYanEconomy.INSTANCE)
                .filterIsInstance(MessageEvent.class)
                .filter(event -> event.getSender().getId() == user.getId());

        CompletableFuture<MessageEvent> future = new CompletableFuture<>();

        channel.subscribeOnce(MessageEvent.class, EmptyCoroutineContext.INSTANCE,
                ConcurrencyKind.LOCKED, EventPriority.HIGH, event -> {
                    if (type) {
                        event.intercept();
                    }
                    future.complete(event);
                }
        );
        MessageEvent event = null;
        try {
            event = future.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.error("获取下一条消息出错!", e);
        }
        assert event != null;
        return event;
    }

}
