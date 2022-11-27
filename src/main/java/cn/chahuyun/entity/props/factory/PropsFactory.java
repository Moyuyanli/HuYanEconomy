package cn.chahuyun.entity.props.factory;

import cn.chahuyun.entity.props.PropsBase;

import java.util.Date;

/**
 * @author Erzbir
 * @Date: 2022/11/27 14:09
 */
public interface PropsFactory {
    PropsBase create(String code, String name, int cost, String description, boolean reuse, Date getTime, Date expiredTime, boolean status, boolean operation, Date enabledTime, String aging);

    PropsBase create();
}
