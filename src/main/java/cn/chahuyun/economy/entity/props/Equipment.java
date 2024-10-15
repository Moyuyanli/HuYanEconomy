package cn.chahuyun.economy.entity.props;

import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.prop.PropBase;

/**
 * 装备
 *
 * @author Moyuyanli
 * @date 2024-10-15 11:35
 */
public class Equipment extends PropBase {
    /**
     * 商店显示描述
     *
     * @return 商店显示结果
     */
    @Override
    public String toShopInfo() {
        return null;
    }

    /**
     * 使用该道具
     *
     * @param user
     */
    @Override
    public void use(UserInfo user) {

    }
}
