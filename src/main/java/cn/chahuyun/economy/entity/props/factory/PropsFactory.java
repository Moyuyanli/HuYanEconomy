package cn.chahuyun.economy.entity.props.factory;

import cn.chahuyun.economy.entity.props.PropsBase;

/**
 * 道具工厂<p>
 * 实现此接口以保证道具的获取方法<p>
 *
 * @author Erzbir
 * @Date: 2022/11/27 14:09
 */
public interface PropsFactory {
    /**
     * 通过 [code] 获取一个道具
     *
     * @param code 道具code
     * @return 对应的道具
     */
    PropsBase create(String code);

    PropsBase create();
}
