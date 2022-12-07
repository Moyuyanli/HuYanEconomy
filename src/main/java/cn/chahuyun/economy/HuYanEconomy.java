package cn.chahuyun.economy;

import cn.chahuyun.config.EconomyConfig;
import cn.chahuyun.economy.event.BotOnlineEventListener;
import cn.chahuyun.economy.event.MessageEventListener;
import cn.chahuyun.economy.manager.LotteryManager;
import cn.chahuyun.economy.plugin.PluginManager;
import cn.chahuyun.economy.util.EconomyUtil;
import cn.chahuyun.economy.util.HibernateUtil;
import cn.chahuyun.economy.util.Log;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.GlobalEventChannel;
import xyz.cssxsh.mirai.hibernate.MiraiHibernateConfiguration;

public final class HuYanEconomy extends JavaPlugin {
    /**
     * 唯一实例
     */
    public static final HuYanEconomy INSTANCE = new HuYanEconomy();
    /**
     * 全局版本
     */
    public static final String version = "0.1.7";
    /**
     * 配置
     */
    public static final EconomyConfig config = EconomyConfig.INSTANCE;
    /**
     * 插件所属bot
     */
    public static Bot bot;

    private HuYanEconomy() {
        super(new JvmPluginDescriptionBuilder("cn.chahuyun.economy.HuYanEconomy", version)
                .name("HuYanEconomy")
                .info("壶言经济")
                .author("Moyuyanli")
                //忽略依赖版本
                .dependsOn("xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin", false)
                .dependsOn("xyz.cssxsh.mirai.plugin.mirai-economy-core", false)
                .dependsOn("cn.chahuyun.HuYanSession", true)
                .build());
    }

    @Override
    public void onEnable() {
        EventChannel<Event> eventEventChannel = GlobalEventChannel.INSTANCE.parentScope(HuYanEconomy.INSTANCE);
        //加载前置
        MiraiHibernateConfiguration configuration = new MiraiHibernateConfiguration(this);
        //初始化插件数据库
        HibernateUtil.init(configuration);
        //插件功能初始化
        PluginManager.init();

        //加载配置
        reloadPluginConfig(config);
        long configBot = config.getBot();
        if (configBot == 0) {
            Log.warning("插件管理机器人还没有配置，请尽快配置!");
        } else {
            EconomyUtil.init();
            LotteryManager.init(true);
            eventEventChannel.registerListenerHost(new BotOnlineEventListener());
            eventEventChannel.registerListenerHost(new MessageEventListener());
            Log.info("事件已监听!");
        }
        Log.info(String.format("HuYanEconomy已加载！当前版本 %s !", version));
    }
}