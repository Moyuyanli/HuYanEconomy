package cn.chahuyun.economy.manager;

import cn.chahuyun.authorize.EventComponent;
import cn.chahuyun.authorize.MessageAuthorize;
import cn.chahuyun.authorize.constant.MessageMatchingEnum;
import cn.chahuyun.economy.constant.TitleCode;
import cn.chahuyun.economy.entity.TitleInfo;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.fish.FishRanking;
import cn.chahuyun.economy.entity.title.TitleTemplate;
import cn.chahuyun.economy.entity.title.TitleTemplateSimpleImpl;
import cn.chahuyun.economy.plugin.TitleTemplateManager;
import cn.chahuyun.economy.utils.EconomyUtil;
import cn.chahuyun.economy.utils.Log;
import cn.chahuyun.economy.utils.MessageUtil;
import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.QuoteReply;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 称号管理
 *
 * @author Moyuyanli
 * @date 2022/12/5 17:02
 */
@EventComponent
public class TitleManager {


    /**
     * 初始化加载称号。<br>
     * 这里有注册称号的案例。<br>
     * 包含两个不可购买称号和一个可购买称号。<br>
     * 详细注册信息查看 {@link TitleTemplateSimpleImpl}
     */
    public static void init() {
        TitleTemplateManager.registerTitleTemplate(
                new TitleTemplateSimpleImpl(TitleCode.SIGN_15, TitleCode.SIGN_15_EXPIRED, "签到狂人",
                        true, false,
                        "[只是个传说]", new Color(0xff7f50), new Color(0xff6348)),
                new TitleTemplateSimpleImpl(TitleCode.MONOPOLY, TitleCode.MONOPOLY_EXPIRED, "大富翁",
                        true, true,
                        "[大富翁]", new Color(0xff4757), new Color(0xffa502)),
                new TitleTemplateSimpleImpl(TitleCode.REGAL, TitleCode.REGAL_EXPIRED, "小富翁",
                        10000.0, true, false,
                        "[小富翁]", new Color(0xECCC68), new Color(0xffa502)),
                new TitleTemplateSimpleImpl(TitleCode.FISHING, TitleCode.FISHING_EXPIRED, "钓鱼佬",
                        true, true,
                        "[邓刚]", new Color(0xf02fc2), new Color(0x6094ea)),
                new TitleTemplateSimpleImpl(TitleCode.BET_MONSTER, TitleCode.BET_MONSTER_EXPIRED, "赌怪",
                        true, true,
                        "[17张牌能秒我?]", new Color(0xFF0000), new Color(0x730000)),
                new TitleTemplateSimpleImpl(TitleCode.ROB, TitleCode.ROB_EXPIRED, "街区传说",
                        false, true,
                        "[师承窃格瓦拉]", new Color(0x2261DC), null));


        //修改版本迭代带来的错误数据
        List<TitleInfo> titleInfos = HibernateFactory.selectList(TitleInfo.class);

        for (TitleInfo titleInfo : titleInfos) {
            if (titleInfo.getCode() == null) {
                switch (titleInfo.getTitle()) {
                    case "[只是个传说]":
                        HibernateFactory.merge(titleInfo.setCode(TitleCode.SIGN_15).setName("签到狂人"));
                        continue;
                    case "[大富翁]":
                        HibernateFactory.merge(titleInfo.setCode(TitleCode.MONOPOLY).setName("大富翁"));
                        continue;
                    case "[小富翁]":
                        HibernateFactory.merge(titleInfo.setCode(TitleCode.REGAL).setName("小富翁"));
                        continue;
                }
            }
        }
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
     * @param userInfo          用户
     * @param titleTemplateCode 称号code
     * @return true 添加成功 false 称号已存在或添加失败。
     */
    public static boolean addTitleInfo(UserInfo userInfo, String titleTemplateCode) {
        TitleInfo title = TitleTemplateManager.createTitle(titleTemplateCode, userInfo);
        if (title == null) {
            throw new RuntimeException("称号code错误或该称号没有在称号模版管理中注册!");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("code", title.getCode());
        params.put("userId", title.getUserId());
        TitleInfo selectOne = HibernateFactory.selectOne(TitleInfo.class, params);
        if (selectOne != null) {
            return false;
        }
        return HibernateFactory.merge(title).getId() != 0;
    }

    /**
     * 查询拥有的称号
     *
     * @param event 消息事件
     */
    @MessageAuthorize(text = {"我的称号","称号列表","拥有称号"})
    public static void viewTitleInfo(MessageEvent event) {
        Log.info("查询称号指令");

        Contact subject = event.getSubject();
        long id = event.getSender().getId();

        MessageChainBuilder builder = new MessageChainBuilder();
        builder.append(new QuoteReply(event.getSource()));

        List<TitleInfo> titleList = HibernateFactory.selectList(TitleInfo.class, "userId", id);
        if (titleList.isEmpty()) {
            subject.sendMessage(builder.append("你还没有称号!").build());
            return;
        }

        builder.append("你拥有的称号如下:\n");
        int index = 0;
        for (TitleInfo titleInfo : titleList) {
            if (checkTitleTime(titleInfo)) {
                continue;
            }
            String titleName = titleInfo.getName();
            if (titleInfo.isStatus()) {
                titleName += ":已启用";
            }
            builder.append(String.format("%d-%s%n", ++index, titleName));
        }
        if (index != 0) {
            subject.sendMessage(builder.build());
        } else {
            subject.sendMessage("你还没有称号!");
        }
    }


    /**
     * 查询拥有的称号
     *
     * @param event 消息事件
     */
    @MessageAuthorize(text = "称号商店")
    public static void viewCanByTitle(MessageEvent event) {
        Log.info("查询称号商店指令");

        MessageChainBuilder builder = new MessageChainBuilder();
        builder.append("可购买的称号如下:\n");
        List<TitleTemplate> canBuyTemplate = TitleTemplateManager.getCanBuyTemplate();
        for (TitleTemplate template : canBuyTemplate) {
            builder.append(String.format("%s - %s 金币-有效期: %s%n", template.getTitleName(), template.getPrice(),
                    template.getValidityPeriod() > 0 ? template.getValidityPeriod() + "天" : "永久"
            ));
        }
        event.getSubject().sendMessage(builder.build());
    }


    /**
     * 购买称号<p/>
     * 购买称号 xxx
     *
     * @param event 消息事件
     */
    @MessageAuthorize(
            text = "购买称号 (\\S+)",
            messageMatching = MessageMatchingEnum.REGULAR
    )
    public static void buyTitle(MessageEvent event) {
        Log.info("购买称号指令");

        Contact subject = event.getSubject();
        MessageChain message = event.getMessage();

        List<TitleTemplate> canBuyTemplate = TitleTemplateManager.getCanBuyTemplate();
        if (canBuyTemplate.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "没有称号售卖!"));
            return;
        }


        String content = message.contentToString();
        User sender = event.getSender();
        for (TitleTemplate template : canBuyTemplate) {
            if (template.getTitleName().equals(content.split(" +")[1])) {
                double moneyByUser = EconomyUtil.getMoneyByUser(sender);
                if (moneyByUser < template.getPrice()) {
                    subject.sendMessage(MessageUtil.formatMessageChain(message,
                            "你的金币不够 %s ,无法购买 %s 称号!", template.getPrice(), template.getTitleName()));
                    return;
                } else {
                    UserInfo userInfo = UserManager.getUserInfo(sender);
                    if (checkTitleIsExist(userInfo, template.getTemplateCode())) {
                        subject.sendMessage(MessageUtil.formatMessageChain(message,
                                "你已经拥有 %s 称号!", template.getTitleName()));
                        return;
                    }
                    if (EconomyUtil.minusMoneyToUser(sender, template.getPrice())) {
                        if (addTitleInfo(userInfo, template.getTemplateCode())) {
                            subject.sendMessage(MessageUtil.formatMessageChain(message,
                                    "你以成功购买 %s 称号,有效期 %s ", template.getTitleName(),
                                    template.getValidityPeriod() <= 0 ? "无限" : template.getValidityPeriod() + "天"
                            ));
                        } else {
                            subject.sendMessage(MessageUtil.formatMessageChain(message, "购买 %s 称号失败", template.getTitleName()));
                        }

                    }
                }
                return;
            }
        }
        subject.sendMessage(MessageUtil.formatMessageChain(message, "没有这个称号!"));
    }


    /**
     * 切换称号
     *
     * @param event 消息
     */
    @MessageAuthorize(
            text = "切换称号 (\\d+)",
            messageMatching = MessageMatchingEnum.REGULAR
    )
    public static void userTitle(MessageEvent event) {
        Log.info("切换称号指令");

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
                subject.sendMessage(String.format("已切换称号为 %s ", titleInfo.getName()));
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
     * 检查该用户是否有该称号
     *
     * @param userInfo  用户
     * @param titleCode 称号code
     * @return true 该用户存在该称号
     */
    public static boolean checkTitleIsExist(UserInfo userInfo, String titleCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userInfo.getQq());
        params.put("code", titleCode);
        TitleInfo titleInfo = HibernateFactory.selectOne(TitleInfo.class, params);
        return titleInfo != null;
    }

    /**
     * 检查该用户的对应称号是否启用
     *
     * @param userInfo  用户
     * @param titleCode 称号code
     * @return true 已启用
     */
    public static boolean checkTitleIsOnEnable(UserInfo userInfo, String titleCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userInfo.getQq());
        params.put("code", titleCode);
        params.put("status", true);
        TitleInfo titleInfo = HibernateFactory.selectOne(TitleInfo.class, params);
        return titleInfo != null;
    }


    /**
     * 检查称号是否过期
     *
     * @param titleInfo 称号
     * @return true 已经过期，同时在数据库中删除该称号。
     */
    private static boolean checkTitleTime(TitleInfo titleInfo) {
        if (titleInfo.getDueTime() != null) {
            if (DateUtil.between(new Date(), titleInfo.getDueTime(), DateUnit.MINUTE, false) < 0) {
                HibernateFactory.delete(titleInfo);
                return true;
            }
        }
        return false;
    }

    //================================== 外部称号检查 ==================================


    /**
     * 检查大富翁称号
     *
     * @param userInfo 用户
     * @param subject  消息载体
     */
    public static void checkMonopoly(UserInfo userInfo, Contact subject) {
        double moneyByUser = EconomyUtil.getMoneyByUser(userInfo.getUser());
        if (moneyByUser > 100000) {
            if (checkTitleIsExist(userInfo, TitleCode.MONOPOLY)) {
                return;
            }
            addTitleInfo(userInfo, TitleCode.MONOPOLY);
            MessageChainBuilder builder = new MessageChainBuilder();
            builder.append(new At(userInfo.getQq()));
            builder.append("恭喜!你的金币数量大于 100000 ,获得永久称号 [大富翁] !");
            subject.sendMessage(builder.build());
        }
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
            if (checkTitleIsExist(userInfo, TitleCode.SIGN_15)) {
                return;
            }
            addTitleInfo(userInfo, TitleCode.SIGN_15);
            MessageChainBuilder builder = new MessageChainBuilder();
            builder.append(new At(userInfo.getQq()));
            builder.append("恭喜!你已经连续签到 15 天,获得15天称号 签到狂人 !");
            subject.sendMessage(builder.build());
        }
    }

    /**
     * 检查钓鱼佬称号
     *
     * @param userInfo 用户信息
     * @param subject  载体
     */
    public static void checkFishTitle(UserInfo userInfo, Contact subject) {
        Map<String, Object> map = new HashMap<>();
        FishRanking fishRanking = HibernateFactory.selectOneByHql(FishRanking.class, "from FishRanking order by money desc limit 1", map);
        if (fishRanking == null || fishRanking.getQq() != userInfo.getQq()) {
            return;
        }
        TitleInfo titleInfo = HibernateFactory.selectOne(TitleInfo.class, "code", TitleCode.FISHING);

        if (checkTitleIsExist(userInfo, TitleCode.FISHING)) {
            return;
        }
        if (addTitleInfo(userInfo, TitleCode.FISHING)) {
            subject.sendMessage(MessageUtil.formatMessageChain(userInfo.getQq(), "恭喜你斩获钓鱼榜榜首!获得钓鱼佬称号!"));
            if (titleInfo != null) {
                HibernateFactory.delete(titleInfo);
            }
        }
    }


}
