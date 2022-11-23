package cn.chahuyun.manager;

import cn.chahuyun.entity.UserInfo;
import cn.chahuyun.util.EconomyUtil;
import cn.chahuyun.util.HibernateUtil;
import cn.chahuyun.util.Log;
import net.mamoe.mirai.contact.AvatarSpec;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

/**
 * 用户管理<p>
 * 用户的添加|查询<p>
 * 背包的查看<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 16:20
 */
public class UserManager {

    private UserManager() {

    }

    /**
     * 获取用户信息<p>
     * 没有的时候自动新建用户<p>
     *
     * @param user 用户
     * @return cn.chahuyun.entity.UserInfo
     * @author Moyuyanli
     * @date 2022/11/14 17:08
     */
    public static UserInfo getUserInfo(User user) {
        long userId = user.getId();
        //查询用户
        try {
            return HibernateUtil.factory.fromTransaction(session -> {
                HibernateCriteriaBuilder builder = session.getCriteriaBuilder();
                JpaCriteriaQuery<UserInfo> query = builder.createQuery(UserInfo.class);
                JpaRoot<UserInfo> from = query.from(UserInfo.class);
                query.select(from);
                query.where(builder.equal(from.get("qq"), userId));
                return session.createQuery(query).getSingleResult();
            });
        } catch (Exception e) {
            //注册用户
            long group = 0;
            if (user instanceof Member) {
                Member member = (Member) user;
                group = member.getGroup().getId();
            }
            UserInfo info = new UserInfo(userId, group, user.getNick(), new Date());
            try {
                return HibernateUtil.factory.fromTransaction(session -> session.merge(info));
            } catch (Exception exception) {
                Log.error("用户管理错误:注册用户失败", e);
                return null;
            }
        }
    }


    /**
     * 查询个人信息<p>
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/11/23 9:37
     */
    public static void getUserInfo(MessageEvent event) {
        Contact subject = event.getSubject();
        User sender = event.getSender();
        MessageChain message = event.getMessage();

        UserInfo userInfo = getUserInfo(sender);
        double moneyByUser = EconomyUtil.getMoneyByUser(sender);

        MessageChainBuilder singleMessages = new MessageChainBuilder();

        try {
            Image image = Contact.uploadImage(subject, new URL(sender.getAvatarUrl(AvatarSpec.LARGE)).openConnection().getInputStream());
            singleMessages.append(image);
        } catch (IOException e) {
            Log.error("用户管理:查询个人信息上传图片出错!",e);
        }
        if (userInfo == null) {
            subject.sendMessage("获取用户信息出错!");
            return;
        }
        singleMessages.append(userInfo.getString()).append(String.format("金币:%s", moneyByUser));
        subject.sendMessage(singleMessages.build());
    }


}
