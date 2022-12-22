package cn.chahuyun.economy.plugin;

import cn.chahuyun.config.ConfigData;
import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.constant.Constant;
import cn.chahuyun.economy.entity.props.PropsCard;
import cn.chahuyun.economy.manager.PropsManager;
import cn.chahuyun.economy.manager.PropsManagerImpl;
import cn.chahuyun.economy.utils.Log;
import cn.hutool.cron.CronUtil;

/**
 * 插件管理<p>
 *
 * @author Moyuyanli
 * @date 2022/11/15 16:03
 */
public class PluginManager {

    /**
     * 是否加载壶言会话插件
     */
    public static boolean isHuYanSessionPlugin;

    /**
     * 插件的道具管理
     */
    private static PropsManager propsManager = new PropsManagerImpl();

    private PluginManager() {
    }

    /**
     * 初始化插件道具系统
     *
     * @author Moyuyanli
     * @date 2022/11/23 10:50
     */
    public static void init() {
        //插件加载的时候启动调度器
        CronUtil.start();
        //加载道具
        PropsCard propsCard = new PropsCard(Constant.SIGN_DOUBLE_SINGLE_CARD, "签到双倍金币卡", 99, true, "张", "不要999，不要599，只要99金币，你的下一次签到将翻倍！", false, null, null, false, null);

        propsManager.registerProps(propsCard);
        try {
            //壶言会话
            HuYanEconomy.INSTANCE.config.setOwner(ConfigData.INSTANCE.getOwner());
            Log.info("检测到壶言会话,已同步主人!");
            isHuYanSessionPlugin = true;
        } catch (NoClassDefFoundError e) {
            isHuYanSessionPlugin = false;
        }


    }


    /**
     * 获取道具管理实现
     *
     * @return 道具管理实现
     */
    public static PropsManager getPropsManager() {
        return propsManager;
    }

    /**
     * 设置道具管理的实现
     *
     * @param propsManager 道具管理类
     */
    public static void setPropsManager(PropsManager propsManager) {
        PluginManager.propsManager = propsManager;
    }
}
