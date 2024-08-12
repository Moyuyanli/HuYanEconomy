package cn.chahuyun.economy;

import cn.chahuyun.economy.command.EconomyCommand;
import cn.chahuyun.economy.config.EconomyConfig;
import cn.chahuyun.economy.config.EconomyPluginConfig;
import cn.chahuyun.economy.config.FishingMsgConfig;
import cn.chahuyun.economy.event.BotOnlineEventListener;
import cn.chahuyun.economy.event.MessageEventListener;
import cn.chahuyun.economy.manager.BankManager;
import cn.chahuyun.economy.manager.LotteryManager;
import cn.chahuyun.economy.manager.TitleManager;
import cn.chahuyun.economy.plugin.FishManager;
import cn.chahuyun.economy.plugin.PluginManager;
import cn.chahuyun.economy.utils.EconomyUtil;
import cn.chahuyun.economy.utils.HibernateUtil;
import cn.chahuyun.economy.utils.Log;
import cn.hutool.cron.CronUtil;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.console.command.CommandManager;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.GlobalEventChannel;

public final class HuYanEconomy extends JavaPlugin {
    /**
     * 唯一实例
     */
    public static final HuYanEconomy INSTANCE = new HuYanEconomy();
    /**
     * 全局版本
     */
    public static final String VERSION = "0.2.2";
    /**
     * 配置
     */
    public static EconomyConfig config;
    /**
     * 钓鱼消息配置
     */
    public static FishingMsgConfig msgConfig;
    /**
     * 插件所属bot
     */
    public Bot bot;

    private HuYanEconomy() {
        super(new JvmPluginDescriptionBuilder("cn.chahuyun.HuYanEconomy", VERSION)
                .name("HuYanEconomy")
                .info("壶言经济")
                .author("Moyuyanli")
                //忽略依赖版本 true 可选依赖 false 必须依赖
                .dependsOn("xyz.cssxsh.mirai.plugin.mirai-economy-core", false)
                .dependsOn("cn.chahuyun.HuYanAuthorize", true)
                .dependsOn("cn.chahuyun.HuYanSession", true)
                .build());
    }

    @Override
    public void onEnable() {
        //加载配置
        reloadPluginConfig(EconomyConfig.INSTANCE);
        reloadPluginConfig(EconomyPluginConfig.INSTANCE);
        reloadPluginConfig(FishingMsgConfig.INSTANCE);
        //注册指令
        CommandManager.INSTANCE.registerCommand(new EconomyCommand(), false);
        config = EconomyConfig.INSTANCE;
        msgConfig = FishingMsgConfig.INSTANCE;
        //插件功能初始化
        PluginManager.init();
        //初始化插件数据库
        HibernateUtil.init(this);

        EventChannel<Event> eventEventChannel = GlobalEventChannel.INSTANCE.parentScope(HuYanEconomy.INSTANCE);

        long configBot = config.getBot();
        if (configBot == 0) {
            Log.warning("插件管理机器人还没有配置，请尽快配置!");
        } else {
            EconomyUtil.init();
            LotteryManager.init(true);
            FishManager.init();
            BankManager.init();
            TitleManager.init();
            eventEventChannel.registerListenerHost(new BotOnlineEventListener());
            eventEventChannel.registerListenerHost(new MessageEventListener());
            Log.info("事件已监听!");
        }
        EconomyPluginConfig.INSTANCE.setFirstStart(false);
        Log.info(String.format("HuYanEconomy已加载！当前版本 %s !", VERSION));
    }

    /**
     * 插件关闭
     */
    @Override
    public void onDisable() {
        CronUtil.stop();
        Log.info("插件已卸载!");
    }
}