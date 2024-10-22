package cn.chahuyun.economy.prop;

import cn.chahuyun.economy.entity.UserBackpack;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.props.PropsData;
import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 道具管理
 *
 * @author Moyuyanli
 * @date 2024/9/13 14:35
 */
public class PropsManager {

    private final static Class<PropsData> t = PropsData.class;
    private final static Map<String, Class<? extends PropBase>> propsClassMap = new HashMap<>();

    private PropsManager() {
    }

    /**
     * 注册道具类型<p/>
     * 道具类型需要继承 {@link PropBase} 类
     *
     * @param kind   道具类型kind
     * @param tClass 道具类
     * @return true 注册成功
     */
    public static <T extends PropBase> boolean registerProp(String kind, Class<T> tClass) {
        if (propsClassMap.containsKey(kind)) {
            return false;
        }

        if (propsClassMap.containsValue(tClass)) {
            return false;
        }

        propsClassMap.put(kind, tClass);
        return true;
    }


    /**
     * 通过道具实例，添加一个道具<p/>
     * 道具添加方法就此一个。
     *
     * @param prop 道具
     * @return 保存成功后的道具id
     */
    public static <T extends PropBase> long addProp(T prop) {
        if (!calibration(prop)) {
            throw new RuntimeException("该道具未注册!");
        }
        return HibernateFactory.merge(serialization(prop)).getId();
    }


    /**
     * 获取一个道具
     *
     * @param kind   道具类型kind
     * @param id     道具id
     * @param tClass 道具类
     * @return 对应的道具实例
     */
    public static <T extends PropBase> T getProp(String kind, Long id, Class<T> tClass) {
        if (calibration(kind, tClass)) {
            throw new RuntimeException("该道具未注册!");
        }

        return deserialization(id, tClass);
    }

    /**
     * 获取一个道具的基本类型
     *
     * @param kind 道具类型
     * @param id   道具id
     * @return 对应的道具实例
     */
    public static PropBase getProp(String kind, Long id) {
        return deserialization(id, propsClassMap.get(kind));
    }

    /**
     * 获取一个道具的基本类型
     *
     * @param backpack 背包物品
     * @return 对应的道具实例
     */
    public static PropBase getProp(UserBackpack backpack) {
        PropBase base = null;
        try {
            base = deserialization(backpack.getPropId(), propsClassMap.get(backpack.getPropKind()));
        } catch (Exception e) {
            HibernateFactory.delete(backpack);
        }
        return base;
    }

    /**
     * 获取一个道具的基本类型
     *
     * @param backpack   背包物品
     * @param propsClass 道具类型
     * @return 对应的道具实例
     */
    @SuppressWarnings("all")
    public static <T extends PropBase> T getProp(UserBackpack backpack, Class<T> propsClass) {
        return (T) deserialization(backpack.getPropId(), propsClass);
    }

    /**
     * 销毁一个道具
     *
     * @param id 道具id
     */
    public static void destroyPros(Long id) {
        PropsData data = HibernateFactory.selectOne(t, id);
        HibernateFactory.delete(data);
    }

    /**
     * 销毁一个道具,同时去除背包关联信息
     *
     * @param propsData 道具数据
     */
    public static void destroyProsInBackpack(PropsData propsData) {
        PropsData data = HibernateFactory.selectOne(t, propsData.getId());
        HibernateFactory.delete(data);

        UserBackpack one = HibernateFactory.selectOne(UserBackpack.class, "propId", propsData.getId());
        HibernateFactory.delete(one);
    }


    /**
     * 更新一个道具
     *
     * @param id   道具id
     * @param prop 道具
     * @return 更新后的道具
     */
    @SuppressWarnings("all")
    public static <T extends PropBase> T updateProp(Long id, T prop) {
        PropsData data = serialization(prop);
        data.setId(id);

        HibernateFactory.merge(data);

        return (T) getProp(prop.getKind(), id);
    }

    /**
     * 用于获取道具类型kind对应的类class
     *
     * @param kind 道具类型kind
     * @return 道具类class
     */
    public static Class<? extends PropBase> shopClass(String kind) {
        return propsClassMap.get(kind);
    }

    /**
     * 这个道具是否注册
     *
     * @param kind 道具类型kind
     * @return true 已经注册
     */
    public static boolean checkCodeExist(String kind) {
        return propsClassMap.containsKey(kind);
    }

    /**
     * 使用并更新道具
     *
     * @param backpack 背包道具
     * @return true 成功
     */
    public static boolean useAndUpdate(UserBackpack backpack, UserInfo user) {
        try {
            PropBase prop = getProp(backpack);
            prop.use(user);
            updateProp(backpack.getPropId(), prop);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 序列化物品数据
     *
     * @param prop 物品
     * @return propData
     */
    public static <T extends PropBase> PropsData serialization(T prop) {

        if (prop.isCanItExpire()) {
            Integer expire = prop.getExpire();
            if (expire != null && expire != 0) {
                prop.setExpiredTime(DateUtil.offsetDay(new Date(), expire));
            } else {
                prop.setExpiredTime(DateUtil.offsetDay(new Date(), 1));
            }
        }

        PropsData data = new PropsData();

        data.setCode(prop.getCode());
        data.setKind(prop.getKind());
        data.setData(JSONUtil.toJsonStr(prop));

        return data;
    }

    public static <T extends PropBase> T deserialization(Long id, Class<T> tClass) {
        PropsData one = HibernateFactory.selectOne(t, id);

        if (one == null) {
            throw new RuntimeException("该道具不存在！");
        }
        JSONConfig jsonConfig = JSONConfig.create().setIgnoreError(true);
        return JSONUtil.toBean(one.getData(), jsonConfig, tClass);
    }


    public static <T extends PropBase> T deserialization(PropsData one, Class<T> tClass) {
        if (one == null) {
            throw new RuntimeException("该道具不存在！");
        }

        return JSONUtil.toBean(one.getData(), tClass);
    }


    //==========================================================


    private static <T extends PropBase> boolean calibration(T prop) {
        return calibration(prop.getKind(), prop.getClass());
    }

    private static <T extends PropBase> boolean calibration(String kind, Class<T> prop) {
        if (kind == null || kind.isBlank() || !propsClassMap.containsKey(kind)) {
            return false;
        }

        Class<?> aClass = propsClassMap.get(kind);

        return aClass.equals(prop);
    }


}
