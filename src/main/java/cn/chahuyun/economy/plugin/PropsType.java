package cn.chahuyun.economy.plugin;

import cn.chahuyun.economy.entity.props.PropsBase;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 已注册道具列表<p>
 * 通过道具管理的注册道具方法进行添加<p>
 *
 * @author Moyuyanli
 * @date 2022/11/15 16:25
 */
public class PropsType {

    /**
     * 已注册道具信息map<p>
     * 通过 [getPropsInfo] 获取道具的信息<p>
     * 后续所有的新道具都应该以此道具模板为准生成<p>
     */
    private static final Map<String, PropsBase> props = new LinkedHashMap<>();
    /**
     * 道具商店的 [code] 映射<p>
     */
    private static final Map<String, String> map = new HashMap<>();

    private PropsType() {
    }


    //烧了我两个小时，还没烧出来
    //2022年11月16日09:34:45 目前的最优解

    /**
     * 获取一个注册的道具类<p>
     *
     * @param propCode 道具code
     * @return T 道具子类接收
     * @author Moyuyanli
     * @date 2022/11/16 15:05
     */
    public static PropsBase getPropsInfo(String propCode) {
        return props.get(propCode);
    }

    /**
     * 注册道具模板<p>
     * 同时添加道具编号映射<p>
     *
     * @param code      道具code
     * @param propsBase 道具模板
     * @author Moyuyanli
     * @date 2022/11/28 10:39
     */
    public static void add(String code, PropsBase propsBase) {
        props.put(code, propsBase);
        map.put(String.valueOf(getProps().size()), code);
    }

    public static Map<String, PropsBase> getProps() {
        return props;
    }

    /**
     * 通过道具 [code] 获取道具注册编号<p>
     * 便于商店的购买<p>
     *
     * @param code 道具code
     * @return 对应的道具编号
     * @author Moyuyanli
     * @date 2022/11/28 10:37
     */
    public static String getNo(String code) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue().equals(code)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 根据商店编号获取道具 [code]
     *
     * @param no 商店编号
     * @return 道具code
     * @author Moyuyanli
     * @date 2022/11/28 15:22
     */
    public static String getCode(String no) {
        if (map.containsKey(no)) {
            return map.get(no);
        }
        for (PropsBase value : props.values()) {
            if (value.getName().equals(no)) {
                return value.getCode();
            }
        }
        return null;
    }


}
