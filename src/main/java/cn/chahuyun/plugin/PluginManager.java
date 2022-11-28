package cn.chahuyun.plugin;

import cn.chahuyun.constant.Constant;
import cn.chahuyun.entity.props.PropsCard;
import cn.chahuyun.manager.PropsManager;
import cn.chahuyun.manager.PropsManagerImpl;

/**
 * 插件管理<p>
 *
 * @author Moyuyanli
 * @date 2022/11/15 16:03
 */
public class PluginManager {

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
        PropsCard propsCard = new PropsCard(Constant.SIGN_DOUBLE_SINGLE_CARD, "签到双倍金币卡", 300, "让你的下一次签到活动的金币翻倍!", false, null, null, false,  null);
        propsManager.registerProps(propsCard);
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
