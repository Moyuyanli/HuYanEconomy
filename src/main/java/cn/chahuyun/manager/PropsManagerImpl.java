package cn.chahuyun.manager;

import cn.chahuyun.entity.UserBackpack;
import cn.chahuyun.entity.UserInfo;
import cn.chahuyun.entity.props.PropsBase;
import cn.chahuyun.plugin.PropsType;
import cn.chahuyun.util.HibernateUtil;
import cn.chahuyun.util.Log;
import cn.hutool.core.util.StrUtil;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.ForwardMessageBuilder;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 道具管理<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:27
 */
public class PropsManagerImpl implements PropsManager {


    /**
     * 注册道具<p>
     * 道具的<p>
     * [code] [name] [cost] [reuse]<p>
     * [description]<p>
     * 不能为空<p>
     *
     * @param propsBase
     */
    @Override
    public boolean registerProps(PropsBase propsBase) {
        String code = null;
        try {
            int cost = propsBase.getCost();
            boolean reuse = propsBase.isReuse();
            code = propsBase.getCode();
            if (StrUtil.isBlankIfStr(code)) {
                return false;
            }
            String description = propsBase.getDescription();
            if (StrUtil.isBlankIfStr(description)) {
                return false;
            }
            String name = propsBase.getName();
            if (StrUtil.isBlankIfStr(name)) {
                return false;
            }
        } catch (Exception e) {
            Log.error("道具管理:注册道具出错!");
            return false;
        }
        PropsType.add(code, propsBase);
        return true;
    }


    /**
     * 获取该用户的所有道具<p>
     *
     * @param userInfo 用户
     * @return List<E> 道具id集合
     */
    @Override
    public <E extends PropsBase> List<E> getPropsByUser(UserInfo userInfo) {
        //todo 获取该用户的所有道具
        return null;
    }


    /**
     * 获取该用户的对应 [code] 的道具<p>
     *
     * @param userInfo 用户
     * @param code     道具编码
     * @param clazz    对应道具的类
     * @return java.util.List<?> 道具集合
     * @author Moyuyanli
     * @date 2022/11/15 15:44
     */
    @Override
    public <E extends PropsBase> List<E> getPropsByUserFromCode(UserInfo userInfo, String code, Class<E> clazz) {
        List<UserBackpack> backpacks = userInfo.getBackpacks();
        if (backpacks == null || backpacks.size() == 0) {
            return new ArrayList<>();
        }
        List<E> propList = new ArrayList<>();
        for (UserBackpack backpack : backpacks) {
            if (backpack.getPropsCode().equals(code)) {
                continue;
            }
            E base = HibernateUtil.factory.fromSession(session -> session.get(clazz, backpack.getPropId()));
            propList.add(base);
        }
        return propList;
    }

    /**
     * 删除 [用户] 对应的 [道具]
     *
     * @param userInfo 用户
     * @param props    用户道具
     * @param clazz    道具类型
     * @return true 成功删除
     */
    @Override
    public <E> boolean deleteProp(UserInfo userInfo, PropsBase props, Class<E> clazz) {
        try {
            return HibernateUtil.factory.fromTransaction(session -> {
                session.remove(props);
                HibernateCriteriaBuilder builder = session.getCriteriaBuilder();
                JpaCriteriaQuery<UserBackpack> query = builder.createQuery(UserBackpack.class);
                JpaRoot<UserBackpack> from = query.from(UserBackpack.class);
                query.select(from);
                query.where(builder.equal(from.get("propsCode"), props.getCode()));
                query.where(builder.equal(from.get("propId"), props.getId()));
                UserBackpack backpack = session.createQuery(query).getSingleResult();
                session.remove(backpack);
                return true;
            });
        } catch (Exception e) {
            Log.error("道具管理:删除道具出错");
            return false;
        }
    }

    /**
     * 查询道具商店
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/11/23 10:36
     */
    @Override
    public void propStore(MessageEvent event) {
        //todo 后期尝试用反射来实现通过扫描道具的继承类实现道具商店
        Contact subject = event.getSubject();
        MessageChain message = event.getMessage();
        User sender = event.getSender();
        Bot bot = event.getBot();


        ForwardMessageBuilder iNodes = new ForwardMessageBuilder(subject);
        ForwardMessageBuilder propCard = new ForwardMessageBuilder(subject);
        iNodes.add(bot, new PlainText("道具商店"));
        propCard.add(bot, new PlainText("道具卡商店"));
        Set<String> strings = PropsType.getProps().keySet();
        for (String string : strings) {
            if (string.startsWith("K-")) {
                String propInfo = String.format("道具编号:%s\n", PropsType.getNo(string));
                propInfo += PropsType.getPropsInfo(string).toString();
                propCard.add(bot, new PlainText(propInfo));
            }
        }

        iNodes.add(bot, propCard.build());
        subject.sendMessage(iNodes.build());
    }
}


