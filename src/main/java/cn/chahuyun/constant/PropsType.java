package cn.chahuyun.constant;

import cn.chahuyun.entity.PropsBase;
import cn.chahuyun.entity.PropsCard;

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

    private PropsType() {
    }

    /**
     * 已注册道具信息map<p>
     * 通过 [getPropsInfo] 获取道具的信息<p>
     * 后续所有的新道具都应该以此道具模板为准生成<p>
     */
    private static final Map<String, PropsBase> props = new LinkedHashMap<>();


    //烧了我两个小时，还没烧出来
    //2022年11月16日09:34:45 目前的最优解

    /**
     * 获取一个注册的道具类
     *
     * @param propCode 道具code
     * @return T 道具子类接收
     * @author Moyuyanli
     * @date 2022/11/16 15:05
     */
    public static <T extends PropsBase> T getPropsInfo(String propCode) {
        return (T) props.get(propCode);
    }


    public static void add(String code, PropsBase propsBase) {
        props.put(code, propsBase);
    }

}
