package cn.chahuyun.manager;

import cn.chahuyun.entity.UserInfo;
import cn.chahuyun.entity.props.PropsBase;
import net.mamoe.mirai.event.events.MessageEvent;

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
     * @return List<PropsBase> 道具id集合
     */
    <E extends PropsBase> List<E> getPropsByUser(UserInfo userInfo);

    /**
     * 获取该用户的对应 [code] 的道具
     *
     * @param userInfo 用户
     * @param code     道具编码
     * @return
     */
    <E extends PropsBase> List<E> getPropsByUserFromCode(UserInfo userInfo, String code, Class<E> clazz);

    /**
     * 删除 [用户] 对应的 [道具]
     *
     * @param userInfo 用户
     * @param props    用户的道具
     * @param clazz    道具类型
     * @return true 成功删除
     */
    <E> boolean deleteProp(UserInfo userInfo, PropsBase props, Class<E> clazz);


    /**
     * 查询道具商店
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/11/23 10:36
     */
    void propStore(MessageEvent event);

}
