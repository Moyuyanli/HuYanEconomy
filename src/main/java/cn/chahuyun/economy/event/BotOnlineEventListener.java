package cn.chahuyun.economy.event;

import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.utils.Log;
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
        HuYanEconomy.INSTANCE.bot = event.getBot();
        Log.info("bot 已上线!");
    }
}
