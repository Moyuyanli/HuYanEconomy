package cn.chahuyun.economy.action;

import cn.chahuyun.authorize.EventComponent;
import cn.chahuyun.authorize.MessageAuthorize;
import cn.chahuyun.economy.constant.ImageDrawXY;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.UserStatus;
import cn.chahuyun.economy.manager.TitleManager;
import cn.chahuyun.economy.manager.UserCoreManager;
import cn.chahuyun.economy.model.props.PropsCard;
import cn.chahuyun.economy.model.yiyan.YiYan;
import cn.chahuyun.economy.plugin.ImageManager;
import cn.chahuyun.economy.plugin.PluginManager;
import cn.chahuyun.economy.plugin.YiYanManager;
import cn.chahuyun.economy.utils.*;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import net.mamoe.mirai.contact.*;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.QuoteReply;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户相关的“消息/事件入口”。
 *
 * 说明：
 * - 纯工具/数据逻辑统一放到 Kotlin `UserCoreManager`（manager 包）。
 * - 这里仅保留 @MessageAuthorize 的指令处理。
 */
@EventComponent
public class UserAction {

    @MessageAuthorize(text = {"个人信息", "info"})
    public void getUserInfoImage(MessageEvent event) {
        Log.info("个人信息指令");

        Contact subject = event.getSubject();
        User sender = event.getSender();

        UserInfo userInfo = UserCoreManager.getUserInfo(sender);

        TitleManager.checkMonopolyJava(userInfo, subject);

        double moneyByUser = EconomyUtil.getMoneyByUser(sender);

        MessageChainBuilder singleMessages = new MessageChainBuilder();

        try {
            Image image = Contact.uploadImage(subject, new URL(sender.getAvatarUrl(AvatarSpec.LARGE)).openConnection().getInputStream());
            singleMessages.append(image);
        } catch (IOException e) {
            Log.error("用户管理:查询个人信息上传图片出错!", e);
        }

        singleMessages.append(userInfo.getString()).append(String.format("金币:%s", moneyByUser));

        BufferedImage userInfoImageBase = UserCoreManager.getUserInfoImageBase(userInfo);
        if (userInfoImageBase == null) {
            subject.sendMessage(singleMessages.build());
            return;
        }

        YiYan yiYan = YiYanManager.getYiyan();
        if (yiYan == null) {
            String str = HttpUtil.get("https://v1.hitokoto.cn");
            Log.debug("yiyan->" + str);
            if (str.isBlank()) {
                yiYan = new YiYan(0, "无", "无", "无");
            } else {
                yiYan = JSONUtil.parseObj(str).toBean(YiYan.class);
            }
        }

        Graphics2D graphics = ImageUtil.getG2d(userInfoImageBase);
        graphics.setColor(Color.black);
        String hitokoto = yiYan.getHitokoto();
        String signature = "--" + (yiYan.getAuthor() == null ? "无铭" : yiYan.getAuthor()) + ":" + yiYan.getFrom();
        if (PluginManager.isCustomImage) {
            graphics.setFont(ImageManager.getCustomFont());
            ImageUtil.drawString(hitokoto, ImageDrawXY.A_WORD.getX(), ImageDrawXY.A_WORD.getY(), 440, graphics);
            graphics.drawString(signature, ImageDrawXY.A_WORD_FAMOUS.getX(), ImageDrawXY.A_WORD_FAMOUS.getY());
        } else {
            graphics.setFont(new Font("黑体", Font.PLAIN, 20));

            String[] yiyan;
            if (hitokoto.length() > 18) {
                yiyan = new String[3];
                yiyan[0] = hitokoto.substring(0, 18);
                yiyan[1] = hitokoto.substring(18);
                yiyan[2] = signature;
            } else {
                yiyan = new String[2];
                yiyan[0] = hitokoto;
                yiyan[1] = signature;
            }

            AtomicInteger x = new AtomicInteger(230);
            for (String s : yiyan) {
                graphics.drawString(s, 520, x.get());
                x.addAndGet(28);
            }
        }

        graphics.dispose();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(userInfoImageBase, "png", stream);
        } catch (IOException e) {
            Log.error("用户管理:个人信息图片发送错误!", e);
            subject.sendMessage(singleMessages.build());
            return;
        }
        Contact.sendImage(subject, new ByteArrayInputStream(stream.toByteArray()));
    }

    @MessageAuthorize(text = {"money", "经济信息", "我的资金"})
    public void moneyInfo(MessageEvent event) {
        Contact subject = event.getSubject();
        MessageChain message = event.getMessage();
        User user = event.getSender();

        double money = EconomyUtil.getMoneyByUser(user);
        double bank = EconomyUtil.getMoneyByBank(user);

        subject.sendMessage(MessageUtil.formatMessageChain(message, "你的经济状况:%n" +
                        "钱包余额:%.1f%n" +
                        "银行存款:%.1f",
                money, bank
        ));
    }

    @MessageAuthorize(text = "出院")
    public void discharge(GroupMessageEvent event) {
        Member user = event.getSender();

        UserInfo userInfo = UserCoreManager.getUserInfo(user);

        MessageChainBuilder builder = new MessageChainBuilder();
        builder.add(new QuoteReply(event.getMessage()));

        Group group = event.getSubject();
        if (UserStatusAction.checkUserInHospital(userInfo)) {
            UserStatus userStatus = UserStatusAction.getUserStatus(userInfo);

            double price = userStatus.getRecoveryTime() * 3;
            double real;

            if (BackpackAction.checkPropInUser(userInfo, PropsCard.HEALTH)) {
                real = ShareUtils.rounding(price * 0.8);
                builder.add(MessageUtil.formatMessage(
                        "你在出院的时候使用的医保卡，医药费打8折。%n" +
                                "原价/实付医药费:%s/%s",
                        price, real
                ));
            } else {
                real = price;
                builder.add(MessageUtil.formatMessage("你出院了！这次只掏了%s的医药费！", real));
            }

            if (EconomyUtil.minusMoneyToUser(user, real)) {
                group.sendMessage(builder.build());
                UserStatusAction.moveHome(userInfo);
            } else {
                group.sendMessage("出院失败!");
            }
            return;
        }
        group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "你不在医院，你出什么院？"));
    }
}


