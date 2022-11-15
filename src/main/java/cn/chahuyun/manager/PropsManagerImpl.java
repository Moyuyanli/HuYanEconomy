package cn.chahuyun.manager;

import cn.chahuyun.entity.PropsBase;
import cn.chahuyun.entity.UserBackpack;
import cn.chahuyun.entity.UserInfo;

import java.util.List;

/**
 * 道具管理<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:27
 */
public class PropsManagerImpl implements PropsManager {


    /**
     * 注册道具
     *
     * @param propsBase
     */
    @Override
    public void registerProps(PropsBase propsBase) {

    }

    /**
     * 获取该用户的所有道具<p>
     *
     * @param userInfo 用户
     * @return List<?> 道具id集合
     */
    @Override
    public List<?> getPropsByUser(UserInfo userInfo) {
        return null;
    }


    /**
     * 获取该用户的对应 [code] 的道具<p>
     *
     * @param userInfo 用户
     * @param code     道具编码
     * @return java.util.List<?> 道具集合
     * @author Moyuyanli
     * @date 2022/11/15 15:44
     */
    @Override
    public List<?> getPropsByUserFromCode(UserInfo userInfo, String code) {
        List<UserBackpack> backpacks = userInfo.getBackpacks();



        return null;
    }
}


