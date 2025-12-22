package cn.chahuyun.economy.model.title;

import cn.chahuyun.economy.entity.TitleInfo;
import cn.chahuyun.economy.entity.UserInfo;

/**
 * 称号 API 接口
 */
public interface TitleApi {

    /**
     * 创建称号信息
     *
     * @param userInfo 用户信息
     * @return 称号信息
     */
    TitleInfo createTitleInfo(UserInfo userInfo);

}

