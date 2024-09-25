package cn.chahuyun.economy.prop;

import cn.chahuyun.economy.utils.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * 道具商店
 *
 * @author Moyuyanli
 * @date 2024/9/14 10:50
 */
public class PropsShop {

    private final static Map<String, PropBase> shop = new HashMap<>();

    /**
     * 添加道具到商店
     *
     * @param code  道具商店编码
     * @param props 道具实例
     */
    public static <T extends PropBase> void addShop(String code, T props) {
        if (shop.containsKey(code)) {
            Log.debug("道具已存在于商店中: " + code);
            return;
        }
        if (!PropsManager.checkCodeExist(props.getKind())) {
            Log.error("道具类型未注册!");
            return;
        }
        shop.put(code, props);
    }

    /**
     * 获取商店中的所有道具信息
     *
     * @return 包含道具商店编码和信息的映射
     */
    public static Map<String, String> getShopInfo() {
        Map<String, String> info = new HashMap<>();
        for (Map.Entry<String, PropBase> entry : shop.entrySet()) {
            info.put(entry.getKey(), entry.getValue().toShopInfo());
        }
        return info;
    }

    /**
     * 根据道具编码获取道具模板
     *
     * @param code 道具商店编码
     * @return 道具实例
     */
    public static PropBase getTemplate(String code) {
        return shop.get(code);
    }

    /**
     * 获取道具
     *
     * @param code   道具商店编码
     * @param tClass 道具实例类型
     * @return 道具
     */
    public static <T extends PropBase> T restore(String code, Class<T> tClass) {
        PropBase propBase = shop.get(code);
        if (propBase == null || !tClass.isInstance(propBase)) {
            throw new RuntimeException("未找到对应code的道具或道具code跟类型不符: " + code);
        }
        return tClass.cast(propBase);
    }


    /**
     * 检查这个道具code添加没有
     *
     * @param code 道具code
     * @return true 已经添加
     */
    public static boolean checkPropExist(String code) {
        return shop.containsKey(code);
    }
}