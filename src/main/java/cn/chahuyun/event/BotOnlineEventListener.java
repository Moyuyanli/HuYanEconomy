package cn.chahuyun.event;

import cn.chahuyun.HuYanEconomy;
import cn.chahuyun.util.Log;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.BotOnlineEvent;
import org.jetbrains.annotations.NotNull;

/**
 * 机器人上线事件
 *
 * @author Moyuyanli
 * @date 2022/11/15 11:00
 */
public class BotOnlineEventListener extends SimpleListenerHost {

    @EventHandler()
    public void onMessage(@NotNull BotOnlineEvent event) {
        Bot bot = event.getBot();
        if (bot.getId() == HuYanEconomy.config.getBot()) {
            HuYanEconomy.bot = bot;
            Log.info("插件管理机器人已上线");
        }
    }
}
