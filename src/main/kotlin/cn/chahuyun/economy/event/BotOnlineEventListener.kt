package cn.chahuyun.economy.event

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.utils.Log
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.BotOnlineEvent

/**
 * 机器人上线事件
 */
class BotOnlineEventListener : SimpleListenerHost() {

    @EventHandler
    fun onMessage(event: BotOnlineEvent) {
        HuYanEconomy.bot = event.bot
        Log.info("bot 已上线!")
    }
}
