package cn.chahuyun.manager;

import cn.chahuyun.entity.UserInfo;
import cn.chahuyun.util.HibernateUtil;
import cn.chahuyun.util.Log;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.User;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

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
                UserInfo singleResult = null;
                try {
                    singleResult = session.createQuery(query).getSingleResult();
                } catch (Exception ignored) {}
                return singleResult;
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
                Log.error("用户管理错误:注册用户失败",e);
                return null;
            }
        }
    }


}
