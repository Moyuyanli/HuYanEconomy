package cn.chahuyun.entity.props.factory;

import cn.chahuyun.entity.props.PropsFish;
import cn.chahuyun.plugin.PropsType;

/**
 * @author Erzbir
 * @Date: 2022/11/27 14:17
 */
public class PropsFishFactory implements PropsFactory {
    public static PropsFishFactory INSTANCE = new PropsFishFactory();

    private PropsFishFactory() {

    }

    @Override
    public PropsFish create(String code) {
        PropsFish fish = (PropsFish) PropsType.getPropsInfo(code);
        return fish;
    }

    @Override
    public PropsFish create() {
        return new PropsFish();
    }
}
