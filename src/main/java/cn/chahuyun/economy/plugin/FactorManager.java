package cn.chahuyun.economy.plugin;

import cn.chahuyun.economy.entity.GlobalFactor;
import cn.chahuyun.economy.entity.UserFactor;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.hibernateplus.HibernateFactory;

/**
 * 因子管理
 *
 * @author Moyuyanli
 * @date 2024/9/26 10:26
 */
public class FactorManager {
    private final static Class<GlobalFactor> t = GlobalFactor.class;
    private final static Class<UserFactor> z = UserFactor.class;

    public static void init() {
        GlobalFactor one = HibernateFactory.selectOneById(t, 1);

        if (one == null) {
            one = new GlobalFactor();
            HibernateFactory.merge(one);
        }

    }

    /**
     * 获取全局因子
     *
     * @return 全局因子
     */
    public static GlobalFactor getGlobalFactor() {
        return HibernateFactory.selectOneById(t, 1);
    }

    /**
     * 更新因子
     *
     * @param factor 全局因子
     */
    public static void merge(GlobalFactor factor) {
        HibernateFactory.merge(factor);
    }

    /**
     * 获取用户因子
     *
     * @param user 用户
     * @return 用户因子
     */
    public static UserFactor getUserFactor(UserInfo user) {
        UserFactor one = HibernateFactory.selectOneById(z, user.getQq());

        if (one == null) {
            one = new UserFactor();
            one.setId(user.getQq());
            return HibernateFactory.merge(one);
        }

        return one;
    }


    /**
     * 更新因子
     *
     * @param factor 用户因子
     */
    public static void merge(UserFactor factor) {
        HibernateFactory.merge(factor);
    }


}
