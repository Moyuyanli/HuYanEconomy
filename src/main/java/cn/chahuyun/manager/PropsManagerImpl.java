package cn.chahuyun.manager;

import cn.chahuyun.entity.UserBackpack;
import cn.chahuyun.entity.UserInfo;
import cn.chahuyun.entity.props.PropsBase;
import cn.chahuyun.entity.props.PropsCard;
import cn.chahuyun.entity.props.factory.PropsCardFactory;
import cn.chahuyun.plugin.PropsType;
import cn.chahuyun.util.EconomyUtil;
import cn.chahuyun.util.HibernateUtil;
import cn.chahuyun.util.Log;
import cn.hutool.core.util.StrUtil;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.*;
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
        String code;
        try {
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
        List<UserBackpack> backpacks = userInfo.getBackpacks();

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

    /**
     * 购买一个道具，加入到用户背包
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/11/28 15:05
     */
    @Override
    public void buyPropFromStore(MessageEvent event) {
        Contact subject = event.getSubject();
        User sender = event.getSender();
        MessageChain message = event.getMessage();

        MessageChainBuilder messages = new MessageChainBuilder();
        messages.append(new QuoteReply(message));

        String code = message.serializeToMiraiCode();

        String[] s = code.split(" ");
        String no = s[1];
        int num = 1;
        if (s.length == 3) {
            num = Integer.parseInt(s[2]);
        }

        String propCode = PropsType.getCode(no);
        Log.info("道具商店:购买道具-Code " + propCode);


        UserInfo userInfo = UserManager.getUserInfo(sender);
        if (userInfo == null) {
            Log.warning("道具商店:获取用户为空！");
            subject.sendMessage("系统出错，请联系主人!");
            return;
        }

        PropsBase propsInfo = PropsType.getPropsInfo(propCode);
        //用户钱包现有余额
        double money = EconomyUtil.getMoneyByUser(sender);
        //购买道具合计金额
        int total = propsInfo.getCost() * num;

        if (money - total < -1000) {
            messages.append(new PlainText("做梦去吧你，" + propsInfo.getName() + " 也是你能想要的东西?"));
            subject.sendMessage(messages.build());
            return;
        } else if (money - total < 0) {
            messages.append(new PlainText("这么点钱就想买 " + propsInfo.getName() + " ?"));
            subject.sendMessage(messages.build());
            return;
        }

        if (!EconomyUtil.lessMoneyToUser(sender, total)) {
            Log.warning("道具商店:减少余额失败!");
            subject.sendMessage("系统出错，请联系主人!");
            return;
        }

        PropsCard propsCard = PropsCardFactory.INSTANCE.create(propCode);

        UserBackpack userBackpack = new UserBackpack(userInfo, propsCard);

        if (!userInfo.addPropToBackpack(userBackpack)) {
            Log.warning("道具商店:添加道具到用户背包失败!");
            subject.sendMessage("系统出错，请联系主人!");
            return;
        }

        money = EconomyUtil.getMoneyByUser(sender);


        messages.append(String.format("成功购买 %s %d%s,你还有 %s 枚金币", propsInfo.getName(), num, propsInfo.getUnit(), money));

        Log.info("道具商店:道具购买成功");

        subject.sendMessage(messages.build());

    }

    /**
     * 查询用户背包
     *
     * @param event 消息事件
     */
    @Override
    public void viewUserBackpack(MessageEvent event) {
        Contact subject = event.getSubject();
        User sender = event.getSender();

        UserInfo userInfo = UserManager.getUserInfo(sender);

        assert userInfo != null;
        List<PropsBase> propsByUser = getPropsByUser(userInfo);

        for (PropsBase propsBase : propsByUser) {

        }


    }



}


