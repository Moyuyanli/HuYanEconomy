package cn.chahuyun.economy.action;

import cn.chahuyun.authorize.EventComponent;
import cn.chahuyun.authorize.MessageAuthorize;
import cn.chahuyun.authorize.constant.MessageMatchingEnum;
import cn.chahuyun.economy.entity.TitleInfo;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.manager.TitleManager;
import cn.chahuyun.economy.manager.UserCoreManager;
import cn.chahuyun.economy.model.title.TitleTemplate;
import cn.chahuyun.economy.plugin.TitleTemplateManager;
import cn.chahuyun.economy.utils.EconomyUtil;
import cn.chahuyun.economy.utils.Log;
import cn.chahuyun.economy.utils.MessageUtil;
import cn.chahuyun.hibernateplus.HibernateFactory;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.QuoteReply;

import java.util.List;

/**
 * 称号管理
 *
 * @author Moyuyanli
 * @date 2022/12/5 17:02
 */
@EventComponent
public class TitleAction {


    /**
     * 查询拥有的称号
     *
     * @param event 消息事件
     */
    @MessageAuthorize(text = {"我的称号", "称号列表", "拥有称号"})
    public void viewTitleInfo(MessageEvent event) {
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
            if (TitleManager.checkTitleTime(titleInfo)) {
                continue;
            }
            String titleName = titleInfo.getName();
            if (titleInfo.getStatus()) {
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
    public void viewCanByTitle(MessageEvent event) {
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
    public void buyTitle(MessageEvent event) {
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
                    UserInfo userInfo = UserCoreManager.getUserInfo(sender);
                    if (TitleManager.checkTitleIsExist(userInfo, template.getTemplateCode())) {
                        subject.sendMessage(MessageUtil.formatMessageChain(message,
                                "你已经拥有 %s 称号!", template.getTitleName()));
                        return;
                    }
                    if (EconomyUtil.minusMoneyToUser(sender, template.getPrice())) {
                        if (TitleManager.addTitleInfo(userInfo, template.getTemplateCode())) {
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
    public void userTitle(MessageEvent event) {
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


}
