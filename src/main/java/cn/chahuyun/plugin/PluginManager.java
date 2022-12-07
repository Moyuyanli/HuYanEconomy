package cn.chahuyun.plugin;

import cn.chahuyun.HuYanEconomy;
import cn.chahuyun.HuYanSession;
import cn.chahuyun.config.ConfigData;
import cn.chahuyun.constant.Constant;
import cn.chahuyun.entity.props.PropsCard;
import cn.chahuyun.manager.PropsManager;
import cn.chahuyun.manager.PropsManagerImpl;
import cn.chahuyun.util.Log;

/**
 * 插件管理<p>
 *
 * @author Moyuyanli
 * @date 2022/11/15 16:03
 */
public class PluginManager {

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
        PropsCard propsCard = new PropsCard(Constant.SIGN_DOUBLE_SINGLE_CARD, "签到双倍金币卡", 99, true, "张", "不要999，不要599，只要99金币，你的下一次签到将翻倍！", false, null, null, false, null);
        propsManager.registerProps(propsCard);
        try {
            //壶言会话
            HuYanSession instance = HuYanSession.INSTANCE;
            HuYanEconomy.config.setOwner(ConfigData.INSTANCE.getOwner());
            Log.info("检测到壶言经济,已同步主人");
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
