package cn.chahuyun.economy;

import cn.chahuyun.authorize.PermissionServer;
import cn.chahuyun.authorize.exception.ExceptionHandle;
import cn.chahuyun.economy.command.EconomyCommand;
import cn.chahuyun.economy.config.EconomyConfig;
import cn.chahuyun.economy.config.EconomyPluginConfig;
import cn.chahuyun.economy.config.FishingMsgConfig;
import cn.chahuyun.economy.config.RobMsgConfig;
import cn.chahuyun.economy.constant.Icon;
import cn.chahuyun.economy.fish.FishRollEvent;
import cn.chahuyun.economy.fish.FishStartEvent;
import cn.chahuyun.economy.manager.*;
import cn.chahuyun.economy.plugin.*;
import cn.chahuyun.economy.sign.SignEvent;
import cn.chahuyun.economy.utils.EconomyUtil;
import cn.chahuyun.economy.utils.HibernateUtil;
import cn.chahuyun.economy.utils.Log;
import cn.chahuyun.economy.utils.ShareUtils;
import cn.hutool.cron.CronUtil;
import kotlin.coroutines.EmptyCoroutineContext;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.console.command.CommandManager;
import net.mamoe.mirai.console.extension.PluginComponentStorage;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.*;
import org.jetbrains.annotations.NotNull;

public final class HuYanEconomy extends JavaPlugin {
    /**
     * 唯一实例
     */
    public static final HuYanEconomy INSTANCE = new HuYanEconomy();
    /**
     * 插件运行状态
     */
    public static boolean PLUGIN_STATUS = false;
    /**
     * 配置
     */
    public static EconomyConfig config;
    /**
     * 钓鱼消息配置
     */
    public static FishingMsgConfig msgConfig;
    /**
     * 抢劫消息配置
     */
    public static RobMsgConfig robConfig;
    /**
     * 插件所属bot
     */
    public Bot bot;

    private HuYanEconomy() {
        super(new JvmPluginDescriptionBuilder("cn.chahuyun.HuYanEconomy", BuildConstants.VERSION)
                .name("HuYanEconomy")
                .info("壶言经济")
                .author("Moyuyanli")
                //忽略依赖版本 true 可选依赖 false 必须依赖
                .dependsOn("xyz.cssxsh.mirai.plugin.mirai-economy-core", ">=1.0.6", false)
                .dependsOn("cn.chahuyun.HuYanAuthorize", ">= 1.2.0", false)
                .dependsOn("cn.chahuyun.HuYanSession", true)
                .build());
    }

    @Override
    public void onLoad(@NotNull PluginComponentStorage $this$onLoad) {
        super.onLoad($this$onLoad);
        PLUGIN_STATUS = true;
    }

    @Override
    public void onEnable() {
        Icon.init(getLogger());
        //加载配置
        reloadPluginConfig(EconomyConfig.INSTANCE);
        reloadPluginConfig(EconomyPluginConfig.INSTANCE);
        reloadPluginConfig(FishingMsgConfig.INSTANCE);
        reloadPluginConfig(RobMsgConfig.INSTANCE);
        //注册指令
        CommandManager.INSTANCE.registerCommand(new EconomyCommand(), false);
        config = EconomyConfig.INSTANCE;
        msgConfig = FishingMsgConfig.INSTANCE;
        robConfig = RobMsgConfig.INSTANCE;
        //插件功能初始化
        PluginManager.init();
        //插件权限code注册
        PermCodeManager.init(this);
        //初始化插件数据库
        HibernateUtil.init(this);
        //初始化消息等待线程池
        ShareUtils.init();

        //功能加载
        EconomyUtil.init();
        LotteryManager.init();
        FishManager.init();
        BankManager.init();
        TitleManager.init();
        YiYanManager.init();
        GamesManager.init();
        FactorManager.init();

        PluginPropsManager.init();

        //注册自定义称号
        TitleTemplateManager.loadingCustomTitle();

        //注册消息
        PermissionServer.INSTANCE.registerMessageEvent(this, "cn.chahuyun.economy.manager", new ExceptionHandle(), EconomyConfig.INSTANCE.getPrefix());


        EventChannel<Event> eventEventChannel = GlobalEventChannel.INSTANCE.parentScope(HuYanEconomy.INSTANCE);

        //监听自定义签到事件
        eventEventChannel.subscribeAlways(SignEvent.class,
                EmptyCoroutineContext.INSTANCE,
                ConcurrencyKind.CONCURRENT,
                EventPriority.HIGH,
                SignManager::randomSignGold
        );

        eventEventChannel.subscribeAlways(SignEvent.class, SignManager::signProp);
        eventEventChannel.subscribeAlways(FishStartEvent.class, GamesManager::fishStart);
        eventEventChannel.subscribeAlways(FishRollEvent.class, GamesManager::fishRoll);

        Log.info("事件已监听!");

        EconomyPluginConfig.INSTANCE.setFirstStart(false);
        Log.info(String.format("HuYanEconomy已加载！当前版本 %s !", getDescription().getVersion()));
    }

    /**
     * 插件关闭
     */
    @Override
    public void onDisable() {
        PLUGIN_STATUS = false;

        CronUtil.stop();
        YiYanManager.shutdown();
        ShareUtils.shutdown();
        Log.info("插件已卸载!");
    }
}