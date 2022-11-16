package cn.chahuyun.entity;

import java.io.Serializable;

/**
 * 鱼道具
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:35
 */
public class PropsFish extends PropsBase implements Serializable {
    /**
     * 创建一个道具
     * 具体实现方法请查看卡道具
     *
     * @param code
     * @return 道具的实现类
     */
    @Override
    public <T extends PropsBase> T getProp(String code) {
        return null;
    }
}
