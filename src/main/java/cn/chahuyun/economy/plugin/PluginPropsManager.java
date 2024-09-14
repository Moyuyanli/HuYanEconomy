package cn.chahuyun.economy.plugin;

import cn.chahuyun.economy.entity.props.PropsCard;
import cn.chahuyun.economy.props.PropsManager;

/**
 * 道具管理
 *
 * @author Moyuyanli
 * @date 2024/9/14 10:46
 */
public class PluginPropsManager {

    public static void init() {
        PropsManager.registerProps(PropsCard.SIGN_2, PropsCard.class);
        PropsManager.registerProps(PropsCard.SIGN_3, PropsCard.class);
    }

}
