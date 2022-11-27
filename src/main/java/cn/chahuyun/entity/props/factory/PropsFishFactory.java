package cn.chahuyun.entity.props.factory;

import cn.chahuyun.entity.props.PropsFish;

import java.util.Date;

/**
 * @author Erzbir
 * @Date: 2022/11/27 14:17
 */
public class PropsFishFactory implements PropsFactory {
    public static PropsFishFactory INSTANCE = new PropsFishFactory();

    private PropsFishFactory() {

    }

    @Override
    public PropsFish create(String code, String name, int cost, String description, boolean reuse, Date getTime, Date expiredTime, boolean status, boolean operation, Date enabledTime, String aging) {
        return new PropsFish(code, name, cost, description, reuse, getTime, expiredTime, status, operation, enabledTime, aging);
    }

    @Override
    public PropsFish create() {
        return new PropsFish();
    }
}
