package cn.chahuyun.economy.manager;

import cn.chahuyun.authorize.EventComponent;
import cn.chahuyun.authorize.MessageAuthorize;
import cn.chahuyun.authorize.constant.MessageMatchingEnum;
import cn.chahuyun.economy.entity.UserBackpack;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.prop.PropBase;
import cn.chahuyun.economy.prop.PropsManager;
import cn.chahuyun.economy.utils.Log;
import cn.chahuyun.economy.utils.MessageUtil;
import cn.chahuyun.hibernateplus.HibernateFactory;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.ForwardMessageBuilder;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * 背包管理
 *
 * @author Moyuyanli
 * @date 2022/11/15 10:00
 */
@EventComponent
public class BackpackManager {


    @MessageAuthorize(
            text = {"我的背包", "backpack"}
    )
    public void viewBackpack(GroupMessageEvent event) {
        Log.info("背包指令");

        Member sender = event.getSender();
        Bot bot = event.getBot();
        Group group = event.getSubject();

        UserInfo userInfo = UserManager.getUserInfo(sender);

        List<UserBackpack> backpacks = userInfo.getBackpacks();

        if (backpacks.isEmpty()) {
            group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "你的背包为空!"));
            return;
        }

        ForwardMessageBuilder iNodes = new ForwardMessageBuilder(group);

        iNodes.add(bot, new PlainText("以下是你的背包↓:"));

        for (UserBackpack backpack : backpacks) {
            PropBase prop = PropsManager.getProp(backpack);
            iNodes.add(bot, new PlainText(String.format("物品id:%d%n%s", backpack.getPropId(), prop.toString())));
        }

        group.sendMessage(iNodes.build());
    }

    @MessageAuthorize(
            text = "use \\d+|使用 \\d+",
            messageMatching = MessageMatchingEnum.REGULAR
    )
    public void useProp(GroupMessageEvent event) {
        Member sender = event.getSender();
        MessageChain message = event.getMessage();
        String content = message.contentToString();
        Group group = event.getSubject();

        long propId = Long.parseLong(content.split(" ")[1]);

        UserInfo userInfo = UserManager.getUserInfo(sender);

        List<UserBackpack> backpacks = userInfo.getBackpacks();
        for (UserBackpack backpack : backpacks) {
            if (backpack.getPropId().equals(propId)) {
                PropBase prop = PropsManager.getProp(backpack);
                prop.use();
                PropsManager.updateProp(backpack.getPropId(), prop);
                group.sendMessage(MessageUtil.formatMessageChain(message, "使用成功!"));
                return;
            }
        }

        group.sendMessage(MessageUtil.formatMessageChain(message, "你没有这个道具!"));
    }

    /**
     * 添加一个道具到背包
     *
     * @param userInfo 用户
     * @param id       道具id
     */
    public static void addPropToBackpack(UserInfo userInfo, String code, String kind, @NotNull Long id) {
        UserBackpack userBackpack = new UserBackpack(userInfo.getId(), code, kind, id);
        userInfo.addPropToBackpack(userBackpack);
    }

    public static void delPropToBackpack(UserInfo userInfo, Long id) {
        List<UserBackpack> backpacks = userInfo.getBackpacks();
        for (UserBackpack backpack : backpacks) {
            if (backpack.getPropId().equals(id)) {
                userInfo.getBackpacks().remove(backpack);
                HibernateFactory.merge(userInfo);
                PropsManager.destroyPros(id);
            }
        }
    }

    /**
     * 检查该用户是否拥有这个道具
     * @param userInfo 用户信息
     * @param id 道具id
     * @return true 拥有
     */
    public static boolean checkPropInUser(UserInfo userInfo, Long id) {
        for (UserBackpack backpack : userInfo.getBackpacks()) {
            if (Objects.equals(backpack.getPropId(), id)) {
                return true;
            }
        }
        return false;
    }

}
