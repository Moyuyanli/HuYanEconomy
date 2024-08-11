package cn.chahuyun.economy.entity.title;

import cn.chahuyun.economy.entity.TitleInfo;
import cn.chahuyun.economy.entity.UserInfo;

/**
 * @author Moyuyanli
 * @Date 2024/8/11 11:30
 */
@FunctionalInterface
public interface TitleApi {

    /**
     * 根据称号模板创建一个称号
     *
     * @param userInfo 用户信息
     * @return 模版创建的称号
     */
    TitleInfo createTitleInfo(UserInfo userInfo);


}