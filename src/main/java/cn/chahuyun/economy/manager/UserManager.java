package cn.chahuyun.economy.manager;

import cn.chahuyun.authorize.EventComponent;
import cn.chahuyun.authorize.MessageAuthorize;
import cn.chahuyun.economy.constant.ImageDrawXY;
import cn.chahuyun.economy.entity.TitleInfo;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.UserStatus;
import cn.chahuyun.economy.model.props.PropsCard;
import cn.chahuyun.economy.model.yiyan.YiYan;
import cn.chahuyun.economy.plugin.ImageManager;
import cn.chahuyun.economy.plugin.PluginManager;
import cn.chahuyun.economy.plugin.YiYanManager;
import cn.chahuyun.economy.utils.*;
import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import net.mamoe.mirai.contact.*;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.QuoteReply;
import org.jetbrains.annotations.NotNull;
import xyz.cssxsh.mirai.economy.service.EconomyAccount;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户管理<p>
 * 用户的添加|查询<p>
 * 背包的查看<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 16:20
 */
@EventComponent
public class UserManager {

    private static int index = 1;


    /**
     * 获取用户信息<p>
     * 没有的时候自动新建用户<p>
     *
     * @param user 用户
     * @return cn.chahuyun.entity.UserInfo
     * @author Moyuyanli
     * @date 2022/11/14 17:08
     */
    @NotNull
    public static UserInfo getUserInfo(User user) {
        long userId = user.getId();
        UserInfo one = HibernateFactory.selectOne(UserInfo.class, "qq", userId);
        Group group = null;
        Member member = null;
        if (user instanceof Member) {
            member = ((Member) user);
            group = member.getGroup();
        }
        if (one == null) {
            UserInfo info = new UserInfo(userId, 0, user.getNick(), new Date());
            info.setRegisterGroup(group != null ? group.getId() : 0);
            info.setUser(user);
            info.setGroup(group);
            return Objects.requireNonNull(HibernateFactory.merge(info));
        }
        one.setUser(user);
        one.setGroup(group);
        return one;
    }

    /**
     * 获取用户信息(不含user)<p>
     * 没有的时候返回null<p>
     *
     * @param account 经济账户
     * @return cn.chahuyun.entity.UserInfo
     * @author Moyuyanli
     * @date 2022/11/14 17:08
     */
    @NotNull
    public static UserInfo getUserInfo(EconomyAccount account) {
        String userId = account.getUuid();
        //查询用户
        try {
            UserInfo userInfo = HibernateFactory.selectOneById(UserInfo.class, userId);
            if (userInfo == null) {
                throw new RuntimeException();
            }
            return userInfo;
        } catch (Exception e) {
            throw new RuntimeException("该经济账号不存在用户信息");
        }
    }


    /**
     * 获取用户信息<p>
     * 可能为空
     *
     * @param userId 用户id
     * @return cn.chahuyun.entity.UserInfo
     * @author Moyuyanli
     * @date 2022/11/14 17:08
     */
    @Nullable
    public static UserInfo getUserInfo(Long userId) {
        return HibernateFactory.selectOne(UserInfo.class, "qq", userId);
    }

    /**
     * 获取用户信息<p>
     * 可能为空
     *
     * @param uuid 用户uuid
     * @return cn.chahuyun.entity.UserInfo
     * @author Moyuyanli
     * @date 2022/11/14 17:08
     */
    @Nullable
    public static UserInfo getUserInfo(String uuid) {
        return HibernateFactory.selectOne(UserInfo.class, "funding", uuid);
    }

    /**
     * 查询个人信息<p>
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/11/23 9:37
     */
    @MessageAuthorize(text = {"个人信息", "info"})
    public void getUserInfoImage(MessageEvent event) {
        Log.info("个人信息指令");

        Contact subject = event.getSubject();
        User sender = event.getSender();

        UserInfo userInfo = getUserInfo(sender);

        TitleManager.checkMonopoly(userInfo, event.getSubject());

        double moneyByUser = EconomyUtil.getMoneyByUser(sender);

        MessageChainBuilder singleMessages = new MessageChainBuilder();

        try {
            Image image = Contact.uploadImage(subject, new URL(sender.getAvatarUrl(AvatarSpec.LARGE)).openConnection().getInputStream());
            singleMessages.append(image);
        } catch (IOException e) {
            Log.error("用户管理:查询个人信息上传图片出错!", e);
        }

        singleMessages.append(userInfo.getString()).append(String.format("金币:%s", moneyByUser));

        BufferedImage userInfoImageBase = getUserInfoImageBase(userInfo);
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
        //图片与文字的抗锯齿
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
                //写入签到信息
                graphics.drawString(s, 520, x.get());
                x.addAndGet(28);
            }
        }


        graphics.dispose();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(userInfoImageBase, "png", stream);
        } catch (IOException e) {
            Log.error("签到管理:签到图片发送错误!", e);
            subject.sendMessage(singleMessages.build());
            return;
        }
        Contact.sendImage(subject, new ByteArrayInputStream(stream.toByteArray()));
    }

    /**
     * 绘制个人信息基础信息<p>
     * 包含:<p>
     * 头像、名称、id、签到时间、连签次数<p>
     * 下次签到时间、称号<p>
     * 总金币、今日签到获得<p>
     *
     * @param userInfo 用户信息
     * @return java.awt.image.BufferedImage
     * @author Moyuyanli
     * @date 2022/12/5 16:11
     */
    public static BufferedImage getUserInfoImageBase(UserInfo userInfo) {
        try {
            return customBottom(userInfo);
        } catch (IOException exception) {
            Log.error("用户管理:个人信息基础信息绘图错误!", exception);
            return null;
        }
    }

    @NotNull
    private static BufferedImage customBottom(UserInfo userInfo) throws IOException {
        BufferedImage bottom = ImageManager.getNextBottom();
        if (bottom == null) {
            throw new IOException("没有自定义底图，请检查data/bottom文件夹底图!");
        }

        User user = userInfo.getUser();

        String avatarUrl = user.getAvatarUrl(AvatarSpec.LARGE);
        BufferedImage avatar = ImageIO.read(new URL(avatarUrl));

        avatar = ImageUtil.makeRoundedCorner(avatar, 50);

        Graphics2D g2d = ImageUtil.getG2d(bottom);

        g2d.drawImage(avatar, ImageDrawXY.AVATAR.getX(), ImageDrawXY.AVATAR.getY(), avatar.getWidth(), avatar.getHeight(), null);

        Font font = ImageManager.getCustomFont();
        g2d.setFont(font);
        g2d.setColor(Color.BLACK);


        String id = String.valueOf(user.getId());
        TitleInfo title = TitleManager.getDefaultTitle(userInfo);
        g2d.setFont(font.deriveFont(32f));
        if (title.getGradient()) {
            ImageUtil.drawStringGradient(title.getTitle(),
                    ImageDrawXY.TITLE.getX(), ImageDrawXY.TITLE.getY(),
                    title.getStartColor(), title.getEndColor(), g2d);
            ImageUtil.drawStringGradient(id,
                    ImageDrawXY.ID.getX(), ImageDrawXY.ID.getY(),
                    title.getStartColor(), title.getEndColor(), g2d);
        } else {
            g2d.setColor(title.getStartColor());
            g2d.drawString(id, ImageDrawXY.ID.getX(), ImageDrawXY.ID.getY());
            g2d.drawString(title.getTitle(), ImageDrawXY.TITLE.getX(), ImageDrawXY.TITLE.getY());
        }

        String nick = user.getNick();
        boolean gradient = false;
        Color sColor = null;
        Color eColor = null;
        if (title.getImpactName()) {
            gradient = true;
            sColor = title.getStartColor();
            eColor = title.getEndColor();
        } else {
            if (user instanceof Member) {
                Member member = (Member) user;
                gradient = true;
                if (member.getPermission() == MemberPermission.OWNER) {
                    sColor = new Color(68, 138, 255);
                    eColor = new Color(100, 255, 218);
                } else if (member.getPermission() == MemberPermission.ADMINISTRATOR) {
                    sColor = new Color(72, 241, 155);
                    eColor = new Color(140, 241, 72);
                } else {
                    sColor = ImageUtil.hexColor("fce38a");
                    eColor = ImageUtil.hexColor("f38181");
                }
            }
        }

        if (nick.length() > 16) {
            g2d.setFont(font.deriveFont(Font.BOLD, 50f));
        } else {
            g2d.setFont(font.deriveFont(Font.BOLD, 60f));
        }

        if (gradient) {
            ImageUtil.drawStringGradient(nick, ImageDrawXY.NICK_NAME.getX(), ImageDrawXY.NICK_NAME.getY(), sColor, eColor, g2d);
            g2d.setColor(Color.BLACK);
        } else {
            g2d.setColor(Color.BLACK);
            g2d.drawString(nick, ImageDrawXY.NICK_NAME.getX(), ImageDrawXY.NICK_NAME.getY());
        }


        String signTime;
        if (userInfo.getSignTime() == null) {
            signTime = "暂未签到";
        } else {
            signTime = DateUtil.format(userInfo.getSignTime(), "yyyy-MM-dd HH:mm:ss");
        }

        g2d.setFont(font.deriveFont(Font.PLAIN, 24f));
        g2d.drawString(signTime, ImageDrawXY.SIGN_TIME.getX(), ImageDrawXY.SIGN_TIME.getY());

        g2d.drawString(String.valueOf(userInfo.getSignNumber()), ImageDrawXY.SIGN_NUM.getX(), ImageDrawXY.SIGN_NUM.getY());

        String money = String.valueOf(EconomyUtil.getMoneyByUser(user));
        String bank = String.valueOf(EconomyUtil.getMoneyByBank(user));

        g2d.setFont(font.deriveFont(32f));
        g2d.drawString(money, ImageDrawXY.MY_MONEY.getX(), ImageDrawXY.MY_MONEY.getY());
        g2d.drawString(String.valueOf(userInfo.getSignEarnings()), ImageDrawXY.SIGN_OBTAIN.getX(), ImageDrawXY.SIGN_OBTAIN.getY());
        g2d.drawString(bank, ImageDrawXY.BANK_MONEY.getX(), ImageDrawXY.BANK_MONEY.getY());
        g2d.drawString(String.valueOf(userInfo.getBankEarnings()), ImageDrawXY.BANK_INTEREST.getX(), ImageDrawXY.BANK_INTEREST.getY());

        g2d.dispose();
        return bottom;
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

    //===============================================================

    @MessageAuthorize(text = "出院")
    public void discharge(GroupMessageEvent event) {
        Member user = event.getSender();

        UserInfo userInfo = getUserInfo(user);

        MessageChainBuilder builder = new MessageChainBuilder();
        builder.add(new QuoteReply(event.getMessage()));

        Group group = event.getSubject();
        if (UserStatusManager.checkUserInHospital(userInfo)) {
            UserStatus userStatus = UserStatusManager.getUserStatus(userInfo);

            double price = userStatus.getRecoveryTime() * 3;
            double real;

            if (BackpackManager.checkPropInUser(userInfo, PropsCard.HEALTH)) {
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
                UserStatusManager.moveHome(userInfo);
            } else {
                group.sendMessage("出院失败!");
            }
            return;
        }
        group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "你不在医院，你出什么院？"));
    }

}
