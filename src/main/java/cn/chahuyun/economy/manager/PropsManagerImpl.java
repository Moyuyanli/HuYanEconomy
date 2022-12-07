package cn.chahuyun.economy.manager;


import cn.chahuyun.economy.entity.UserBackpack;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.props.PropsBase;
import cn.chahuyun.economy.entity.props.PropsCard;
import cn.chahuyun.economy.entity.props.factory.PropsCardFactory;
import cn.chahuyun.economy.plugin.PropsType;
import cn.chahuyun.economy.util.EconomyUtil;
import cn.chahuyun.economy.util.HibernateUtil;
import cn.chahuyun.economy.util.Log;
import cn.hutool.core.util.StrUtil;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.*;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import java.util.*;

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
    public List<PropsBase> getPropsByUser(UserInfo userInfo) {
        //todo 获取该用户的所有道具
        List<PropsBase> props = new ArrayList<>();

        List<UserBackpack> backpacks = userInfo.getBackpacks();
        for (UserBackpack backpack : backpacks) {
            Class<? extends PropsBase> aClass;
            try {
                aClass = (Class<? extends PropsBase>) Class.forName(backpack.getClassName());
            } catch (ClassNotFoundException e) {
                Log.error("道具管理:获取所有道具-获取道具子类出错!", e);
                continue;
            }
            PropsBase fromSession = HibernateUtil.factory.fromSession(session -> session.get(aClass, backpack.getPropId()));
            props.add(fromSession);
        }
        return props;
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
//            if (backpack.getPropsCode().equals(code)) {
//                continue;
//            }
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
     * 查询道具系统
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/11/23 10:36
     */
    @Override
    public void propStore(MessageEvent event) {
        //todo 后期尝试用反射来实现通过扫描道具的继承类实现道具系统
        Contact subject = event.getSubject();
        Bot bot = event.getBot();


        ForwardMessageBuilder iNodes = new ForwardMessageBuilder(subject);
        ForwardMessageBuilder propCard = new ForwardMessageBuilder(subject);
        iNodes.add(bot, new PlainText("道具系统"));
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
        if (propCode == null) {
            Log.warning("道具系统:购买道具为空");
            subject.sendMessage("我这里不卖这个...");
            return;
        }
        Log.info("道具系统:购买道具-Code " + propCode);


        UserInfo userInfo = UserManager.getUserInfo(sender);
        if (userInfo == null) {
            Log.warning("道具系统:获取用户为空！");
            subject.sendMessage("系统出错，请联系主人!");
            return;
        }

        PropsBase propsInfo = PropsType.getPropsInfo(propCode);
        //用户钱包现有余额
        double money = EconomyUtil.getMoneyByUser(sender);
        //购买道具合计金额
        int total = propsInfo.getCost() * num;

        if (money - total < -propsInfo.getCost() * 5) {
            messages.append(new PlainText("做梦去吧你，" + propsInfo.getName() + "也是你能想要的东西?"));
            subject.sendMessage(messages.build());
            return;
        } else if (money - total < 0) {
            messages.append(new PlainText("这么点钱就想买" + propsInfo.getName() + "?"));
            subject.sendMessage(messages.build());
            return;
        }

        if (!EconomyUtil.lessMoneyToUser(sender, total)) {
            Log.warning("道具系统:减少余额失败!");
            subject.sendMessage("系统出错，请联系主人!");
            return;
        }
        PropsCard propsCard = null;
        if (propCode.startsWith("K-")) {
            propsCard = PropsCardFactory.INSTANCE.create(propCode);
        }
        if (propsCard == null) {
            Log.error("道具系统:道具创建为空");
            return;
        }
        int number = num;
        while (number != 0) {
            UserBackpack userBackpack = new UserBackpack(userInfo, propsCard);
            if (!userInfo.addPropToBackpack(userBackpack)) {
                Log.warning("道具系统:添加道具到用户背包失败!");
                subject.sendMessage("系统出错，请联系主人!");
                return;
            }
            number--;
        }

        money = EconomyUtil.getMoneyByUser(sender);

        messages.append(String.format("成功购买 %s %d%s,你还有 %s 枚金币", propsInfo.getName(), num, propsInfo.getUnit(), money));

        Log.info("道具系统:道具购买成功");

        subject.sendMessage(messages.build());

    }

    /**
     * 使用一个道具
     *
     * @param event 消息事件
     */
    @Override
    public void userProp(MessageEvent event) {
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
        Log.info("道具系统:使用道具-Code " + propCode);

        UserInfo userInfo = UserManager.getUserInfo(sender);

        int success = 0;
        PropsBase prop = null;

        if (propCode.startsWith("K-")) {
            assert userInfo != null;
            List<PropsCard> propsByUserFromCode = getPropsByUserFromCode(userInfo, propCode, PropsCard.class);
            if (propsByUserFromCode.size() == 0) {
                subject.sendMessage(messages.append("你的包里没有这个道具!").build());
                return;
            }

            for (PropsCard propsCard : propsByUserFromCode) {
                prop = propsCard;
                if (propsCard.isStatus()) {
                    continue;
                }
                if (num == 0) {
                    break;
                }
                propsCard.setStatus(true);
                propsCard.setEnabledTime(new Date());
                HibernateUtil.factory.fromTransaction(session -> session.merge(propsCard));
                num--;
                success++;
            }
        }
        assert prop != null;
        if (success == 0) {
            subject.sendMessage(messages.append(String.format("你没有未使用的%s", prop.getName())).build());
            return;
        }
        subject.sendMessage(messages.append(String.format("成功使用%d%s%s", success, prop.getUnit(), prop.getName())).build());
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
        Bot bot = event.getBot();

        UserInfo userInfo = UserManager.getUserInfo(sender);

        assert userInfo != null;
        List<PropsBase> propsByUser = getPropsByUser(userInfo);
        if (propsByUser.size() == 0) {
            subject.sendMessage("你的背包空荡荡的...");
            return;
        }

        ForwardMessageBuilder iNodes = new ForwardMessageBuilder(subject);

        Map<String, List<PropsBase>> propsListMap = new HashMap<>();
        List<PropsBase> propsBaseList = new ArrayList<>();

        for (PropsBase propsBase : propsByUser) {
            String code = propsBase.getCode();
            if (propsBase.isStack()) {
                if (propsListMap.containsKey(code)) {
                    propsListMap.get(code).add(propsBase);
                } else {
                    propsListMap.put(code, new ArrayList<>() {{
                        add(propsBase);
                    }});
                }
            } else {
                propsBaseList.add(propsBase);
            }
        }

        for (Map.Entry<String, List<PropsBase>> stringListEntry : propsListMap.entrySet()) {
            PropsBase propsInfo = PropsType.getPropsInfo(stringListEntry.getKey());
            String no = PropsType.getNo(stringListEntry.getKey());
            List<PropsBase> value = stringListEntry.getValue();
            int size = value.size();
            int open = 0;
            if (propsInfo instanceof PropsCard) {
                for (PropsBase propsBase : value) {
                    PropsCard propsCard = (PropsCard) propsBase;
                    if (propsCard.isStatus()) {
                        open++;
                    }
                }
            }
            String format = String.format("道具编号:%s\n道具名称:%s\n道具描述:%s\n总数量:%d\n启用数量:%d", no, propsInfo.getName(), propsInfo.getDescription(), size, open);
            iNodes.add(bot, new PlainText(format));
        }

        for (PropsBase propsBase : propsBaseList) {
            String no = PropsType.getNo(propsBase.getCode());
            String format = String.format("道具编号:%s\n道具名称:%s\n道具描述:%s", no, propsBase.getName(), propsBase.getDescription());
            iNodes.add(bot, new PlainText(format));
        }

        subject.sendMessage(iNodes.build());

    }


}


