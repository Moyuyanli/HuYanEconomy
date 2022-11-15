package cn.chahuyun.constant;

import cn.chahuyun.entity.PropsBase;

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

    private PropsType(){}

    /**
     * 已注册道具信息map<p>
     * 通过 [getPropsInfo] 获取道具的信息<p>
     * 后续所有的新道具都应该以此道具模板为准生成<p>
     */
    public static final Map<String, PropsBase> props = new LinkedHashMap<>();
    public static final Map<String, Class<? extends PropsBase>> propsClass = new LinkedHashMap<>();


    //烧了我两个小时，还没烧出来
    public static <T extends PropsBase> T  getPropsInfo(String propCode,Class<T> tClass) {
        PropsBase base = props.get(propCode);
        Class<? extends PropsBase> aClass = base.getClass();
        return aClass.cast(base);
    }


    public static void a() {
        PropsBase propsInfo = getPropsInfo("a");
    }


}
