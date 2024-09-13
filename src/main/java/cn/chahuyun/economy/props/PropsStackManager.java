package cn.chahuyun.economy.props;

import cn.chahuyun.economy.entity.props.PropsData;
import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.json.JSONUtil;

/**
 * 物品堆栈管理
 *
 * @author Moyuyanli
 * @date 2024/9/13 16:04
 */
class PropsStackManager {

    private final static Class<PropsData> t = PropsData.class;

    private PropsStackManager() {
    }

    public static <T extends PropsBase> PropsData serialization(T props) {
        String code = props.getCode();

        PropsData data = HibernateFactory.selectOne(t, "code", code);

        if (data == null) {
             data = new PropsData();
             data.setCode(code);
             data.setNum(1);
             data.setStack(props.isStack());
             data.setData(JSONUtil.toJsonStr(props));
        }

        if (data.getStack()) {
            data.setNum(data.getNum() + props.getNum()); // 更新数量
        }

        return data;
    }

    public static <T extends PropsBase> T deserialization(Long id, Class<T> tClass) {
        PropsData one = HibernateFactory.selectOne(t, id);

        if (one == null) {
            return null;
        }

        T data = JSONUtil.toBean(one.getData(), tClass);
        data.setNum(data.getNum()); // 设置数量
        return data;
    }

}
