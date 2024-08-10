package cn.chahuyun.economy.manager;

import cn.chahuyun.economy.constant.TitleTemplate;
import cn.chahuyun.economy.entity.TitleInfo;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.utils.EconomyUtil;
import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 称号管理
 *
 * @author Moyuyanli
 * @date 2022/12/5 17:02
 */
public class TitleManager {

    private static final Map<TitleTemplate, TitleInfo> titleInfoMap = new HashMap<>(2);


    private TitleManager() {
    }


    public static void init() {
        titleInfoMap.put(TitleTemplate.SIGN_IN_MADMAN, TitleInfo.builder()
                .gradient(true)
                .impactName(false)
                .type(TitleTemplate.SIGN_IN_MADMAN)
                .title("[只是个传说]")
                .sColor("ff7f50")
                .eColor("ff6348").build());
        titleInfoMap.put(TitleTemplate.MONOPOLY, TitleInfo.builder()
                .gradient(true)
                .impactName(true)
                .type(TitleTemplate.MONOPOLY)
                .title("[大富翁]")
                .sColor("ff4757")
                .eColor("ffa502").build());
        titleInfoMap.put(TitleTemplate.LITTLE_RICH_MAN, TitleInfo.builder()
                .gradient(true)
                .impactName(false)
                .type(TitleTemplate.LITTLE_RICH_MAN)
                .title("[小富翁]")
                .sColor("eccc68")
                .eColor("ffa502").build());
    }

    /**
     * 获取默认称号
     *
     * @param userInfo 用户信息
     * @return 称号
     */
    public static TitleInfo getDefaultTitle(UserInfo userInfo) {
        List<TitleInfo> titleList = HibernateFactory.selectList(TitleInfo.class, "userId", userInfo.getQq());
        if (!titleList.isEmpty()) {
            for (TitleInfo info : titleList) {
                if (checkTitleTime(info)) {
                    continue;
                }
                if (info.isStatus()) {
                    return info;
                }
            }
        }
        return getInfo(userInfo);
    }

    @NotNull
    private static TitleInfo getInfo(UserInfo userInfo) {
        TitleInfo titleInfo = new TitleInfo();
        titleInfo.setGradient(false);
        User user = userInfo.getUser();
        if (user instanceof Member) {
            Member member = (Member) user;
            String title = member.getSpecialTitle();
            String color;
            if (title.isBlank()) {
                title = member.getRankTitle();
                color = "8a8886";
                if (title.isBlank()) {
                    title = "[无]";
                }
            } else {
                color = "ff00ff";
            }
            titleInfo.setTitle(String.format("[%s]", title));
            titleInfo.setSColor(color);
        } else {
            titleInfo.setTitle("[无]");
            titleInfo.setSColor("ff00ff");
        }
        return titleInfo;
    }

    /**
     * 添加称号
     *
     * @param userInfo 用户
     * @param template 称号
     */
    public static void addTitleInfo(UserInfo userInfo, TitleTemplate template) {
        TitleInfo titleInfo = titleInfoMap.get(template);
        titleInfo.setUserId(userInfo.getQq());
        titleInfo.setStatus(true);
        if (template.getValidityPeriod() > 0) {
            DateTime dateTime = DateUtil.offsetDay(new Date(), template.getValidityPeriod());
            titleInfo.setDueTime(dateTime);
        }
        HibernateFactory.merge(titleInfo);
    }

    /**
     * 查询拥有的称号
     *
     * @param event 消息事件
     */
    public static void viewTitleInfo(MessageEvent event) {
        Contact subject = event.getSubject();
        long id = event.getSender().getId();

        List<TitleInfo> titleList = HibernateFactory.selectList(TitleInfo.class, "userId", id);
        if (titleList.isEmpty()) {
            subject.sendMessage("你的称号为空!");
            return;
        }

        MessageChainBuilder builder = new MessageChainBuilder();
        builder.append("你拥有的称号如下:\n");
        int index = 0;
        for (TitleInfo titleInfo : titleList) {
            if (checkTitleTime(titleInfo)) {
                continue;
            }
            builder.append(String.format("%d-%s%n", ++index, titleInfo.getTitle()));
        }
        if (index != 0) {
            subject.sendMessage(builder.build());
        } else {
            subject.sendMessage("你的称号为空!");
        }
    }

    /**
     * 购买称号<p/>
     * 购买称号 xxx
     *
     * @param event 消息事件
     */
    public static void buyTitle(MessageEvent event) {
        ArrayList<String> list = new ArrayList<>() {{
            add("小富翁");
        }};

        Contact subject = event.getSubject();
        User sender = event.getSender();
        MessageChain message = event.getMessage();
        String content = message.contentToString();

        String[] split = content.split(" +");
        if (!list.contains(split[1])) {
            subject.sendMessage("没有这个称号!");
            return;
        }

        UserInfo userInfo = UserManager.getUserInfo(sender);
        switch (split[1]) {
            case "小富翁":
                double moneyByUser = EconomyUtil.getMoneyByUser(sender);
                if (moneyByUser < 10000) {
                    subject.sendMessage("你的金币不够 10000 ,无法购买小富翁称号!");
                } else {
                    if (EconomyUtil.minusMoneyToUser(sender, 10000)) {
                        addTitleInfo(userInfo, TitleTemplate.LITTLE_RICH_MAN);
                        subject.sendMessage("你以成功购买 小富翁 称号,有效期 15 天");
                    }
                }
                return;
        }
    }


    /**
     * 切换称号
     *
     * @param event 消息
     */
    public static void userTitle(MessageEvent event) {
        User user = event.getSender();
        Contact subject = event.getSubject();
        MessageChain message = event.getMessage();
        String content = message.contentToString();

        String[] split = content.split(" +");
        int i = Integer.parseInt(split[1]);

        List<TitleInfo> titleInfos = HibernateFactory.selectList(TitleInfo.class, "userId", user.getId());
        if (titleInfos.isEmpty()) {
            subject.sendMessage("你的称号为空!");
            return;
        }

        int index = 0;
        for (TitleInfo titleInfo : titleInfos) {
            if (++index == i) {
                titleInfo.setStatus(true);
                HibernateFactory.merge(titleInfo);
                subject.sendMessage(String.format("已切换称号为 %s ", titleInfo.getTitle()));
            } else {
                titleInfo.setStatus(false);
                HibernateFactory.merge(titleInfo);
            }
        }

        if (i == 0) {
            subject.sendMessage("已切换为默认称号!");
        }
    }


    /**
     * 检查大富翁称号
     *
     * @param userInfo 用户
     * @param subject  消息载体
     */
    public static void checkMonopoly(UserInfo userInfo, Contact subject) {
        double moneyByUser = EconomyUtil.getMoneyByUser(userInfo.getUser());
        if (moneyByUser > 100000) {
            Map<String, Object> params = new HashMap<>();
            params.put("userId", userInfo.getQq());
            params.put("type", TitleTemplate.MONOPOLY);
            TitleInfo titleInfo = HibernateFactory.selectOne(TitleInfo.class, params);
            if (titleInfo == null) {
                addTitleInfo(userInfo, TitleTemplate.MONOPOLY);
                MessageChainBuilder builder = new MessageChainBuilder();
                builder.append(new At(userInfo.getQq()));
                builder.append("恭喜!你的金币数量大于 100000 ,获得永久称号 [大富翁] !");
                subject.sendMessage(builder.build());
            }
        }
    }

    /**
     * 检查大富翁称号
     *
     * @param userInfo 用户
     */
    public static boolean checkMonopoly(UserInfo userInfo) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userInfo.getQq());
        params.put("type", "0");
        TitleInfo titleInfo = HibernateFactory.selectOne(TitleInfo.class, params);
        return titleInfo != null;
    }


    /**
     * 检查连续签到称号
     *
     * @param userInfo 用户信息
     * @param subject  载体
     */
    public static void checkSignTitle(UserInfo userInfo, Contact subject) {
        int signNumber = userInfo.getSignNumber();
        if (signNumber == 15) {
            List<TitleInfo> titleInfo = HibernateFactory.selectList(TitleInfo.class, "userId", userInfo.getQq());
            boolean in = true;
            for (TitleInfo info : titleInfo) {
                if (info.getType() == TitleTemplate.SIGN_IN_MADMAN) {
                    in = false;
                    break;
                }
            }
            if (in) {
                addTitleInfo(userInfo, TitleTemplate.SIGN_IN_MADMAN);
                MessageChainBuilder builder = new MessageChainBuilder();
                builder.append(new At(userInfo.getQq()));
                builder.append("恭喜!你已经连续签到 15 天,获得15天称号 [签到狂人] !");
                subject.sendMessage(builder.build());
            }
        }
    }


    private static boolean checkTitleTime(TitleInfo titleInfo) {
        if (titleInfo.getDueTime() != null) {
            if (DateUtil.between(new Date(), titleInfo.getDueTime(), DateUnit.MINUTE, false) < 0) {
                HibernateFactory.delete(titleInfo);
                return true;
            }
        }
        return false;
    }
}
