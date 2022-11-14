package cn.chahuyun.event;

import kotlin.coroutines.CoroutineContext;
import net.mamoe.mirai.contact.BotIsBeingMutedException;
import net.mamoe.mirai.contact.MessageTooLargeException;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.EventCancelledException;
import net.mamoe.mirai.event.events.MessageEvent;
import org.jetbrains.annotations.NotNull;

import static cn.chahuyun.HuYanEconomy.log;

/**
 * 说明
 *
 * @author Moyuyanli
 * @Description :消息检测
 * @Date 2022/7/9 18:11
 */
public class MessageEventListener extends SimpleListenerHost {


    @Override
    public void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception) {
        if (exception instanceof EventCancelledException) {
            log.error("发送消息被取消:", exception);
        } else if (exception instanceof BotIsBeingMutedException) {
            log.error("你的机器人被禁言:", exception);
        } else if (exception instanceof MessageTooLargeException) {
            log.error("发送消息过长:", exception);
        } else if (exception instanceof IllegalArgumentException) {
            log.error("发送消息为空:", exception);
        }

        // 处理事件处理时抛出的异常
        log.error("出错啦~\n" + exception.getMessage(), exception);
    }

    /**
     * 消息入口
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/11/14 12:34
     */
    @EventHandler()
    public void onMessage(@NotNull MessageEvent event) throws Exception {

    }

}
