package cn.chahuyun.economy.props;

import cn.chahuyun.economy.entity.props.PropsData;
import cn.chahuyun.hibernateplus.HibernateFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 道具管理
 *
 * @author Moyuyanli
 * @date 2024/9/13 14:35
 */
public class PropsManager {

    private static final Map<String, Class<? extends PropsBase>> propsClassMap = new HashMap<>();

    private PropsManager() {
    }

    public static <T extends PropsBase> boolean registerProps(String code, Class<T> tClass) {
        if (propsClassMap.containsKey(code)) {
            return false;
        }

        propsClassMap.put(code, tClass);
        return true;
    }


    public static <T extends PropsBase> long addProps(T props) {
        if (calibration(props)) {
            throw new RuntimeException("该道具未注册!");
        }
        PropsData data = PropsStackManager.serialization(props);
        return HibernateFactory.merge(data).getId();
    }


    public static <T extends PropsBase> T getProps(String code, Long id, Class<T> tClass) {
        if (calibration(code, tClass)) {
            throw new RuntimeException("该道具未注册!");
        }

        return PropsStackManager.deserialization(id, tClass);
    }


    private static <T extends PropsBase> boolean calibration(T props) {
        return calibration(props.getCode(), props.getClass());
    }

    private static <T extends PropsBase> boolean calibration(String code, Class<T> props) {
        if (code == null || code.isBlank() || !propsClassMap.containsKey(code)) {
            return false;
        }

        Class<?> aClass = propsClassMap.get(code);

        return aClass.equals(props);
    }

    public static Class<? extends PropsBase> shopClass(String code) {
        return propsClassMap.get(code);
    }
}
