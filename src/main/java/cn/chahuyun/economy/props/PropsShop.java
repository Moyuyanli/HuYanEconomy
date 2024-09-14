package cn.chahuyun.economy.props;

import java.util.HashMap;
import java.util.Map;

/**
 * 道具商店
 *
 * @author Moyuyanli
 * @date 2024/9/14 10:50
 */
public class PropsShop {

    private final static Map<String, PropsBase> shop = new HashMap<>();

    /**
     * 添加道具到商店
     *
     * @param code 道具编码
     * @param props 道具实例
     */
    public static <T extends PropsBase> void addShop(String code, T props) {
        if (shop.containsKey(code)) {
            // 如果已经存在，则可以选择覆盖或抛出异常等
            System.out.println("道具已存在于商店中: " + code);
            return;
        }
        shop.put(code, props);
    }

    /**
     * 获取商店中的所有道具信息
     *
     * @return 包含道具编码和信息的映射
     */
    public static Map<String, String> getShopInfo() {
        Map<String, String> info = new HashMap<>();
        for (Map.Entry<String, PropsBase> entry : shop.entrySet()) {
            info.put(entry.getKey(), entry.getValue().toShopInfo());
        }
        return info;
    }

    /**
     * 根据道具编码获取道具模板
     *
     * @param code 道具编码
     * @return 道具实例
     */
    public static PropsBase getTemplate(String code) {
        return shop.get(code);
    }
}