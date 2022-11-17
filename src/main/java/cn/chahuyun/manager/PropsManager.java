package cn.chahuyun.manager;

import cn.chahuyun.entity.PropsBase;
import cn.chahuyun.entity.UserInfo;

import java.util.List;

/**
 * 道具管理
 *
 * @author Moyuyanli
 * @date 2022/11/15 15:36
 */
public interface PropsManager {


    /**
     * 注册道具
     *
     * @param propsBase
     */
    boolean registerProps(PropsBase propsBase);


    /**
     * 获取该用户的所有道具
     *
     * @param userInfo 用户
     * @return List<?> 道具id集合
     */
    List<?> getPropsByUser(UserInfo userInfo);

    /**
     * 获取该用户的对应 [code] 的道具
     *
     * @param userInfo 用户
     * @param code     道具编码
     * @return
     */
    List<? extends PropsBase> getPropsByUserFromCode(UserInfo userInfo, String code, Class<? extends PropsBase> clazz);


}
