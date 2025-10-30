package cn.chahuyun.economy.manager;

import cn.chahuyun.authorize.EventComponent;
import cn.chahuyun.authorize.MessageAuthorize;
import cn.chahuyun.authorize.constant.MessageMatchingEnum;
import cn.chahuyun.economy.entity.UserBackpack;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.props.UseEvent;
import cn.chahuyun.economy.exception.Operation;
import cn.chahuyun.economy.prop.PropBase;
import cn.chahuyun.economy.prop.PropsManager;
import cn.chahuyun.economy.utils.Log;
import cn.chahuyun.economy.utils.MessageUtil;
import lombok.val;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.*;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
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

        var index = 1;
        var pageSize = 30;
        var totalSize = backpacks.size();
        var maxIndex = (int) Math.ceil((double) totalSize / pageSize);

        int fromIndex = 0;
        int toIndex = Math.min(index * pageSize, totalSize);
        var currentBackpacks = backpacks.subList(fromIndex, toIndex);
        showBackpack(bot, currentBackpacks, group, index, maxIndex);

        while (true) {
            GroupMessageEvent nextMessage = MessageUtil.INSTANCE.nextUserForGroupMessageEventSync(group.getId(), sender.getId(), 30);
            if (nextMessage == null || !nextMessage.getMessage().contentToString().matches("[上下]一页")) {
                return;
            }
            val string = nextMessage.getMessage().contentToString();
            boolean shouldUpdate = false;

            if (string.equals("上一页") && index > 1) {
                index--;
                shouldUpdate = true;
            } else if (string.equals("下一页") && index < maxIndex) {
                index++;
                shouldUpdate = true;
            }

            if (shouldUpdate) {
                fromIndex = (index - 1) * pageSize;
                toIndex = Math.min(index * pageSize, totalSize);
                currentBackpacks = backpacks.subList(fromIndex, toIndex);
                showBackpack(bot, currentBackpacks, group, index, maxIndex);
            }
        }
    }

    private static void showBackpack(Bot bot, List<UserBackpack> backpacks, Group group, int currentPage, int maxPage) {
        ForwardMessageBuilder iNodes = new ForwardMessageBuilder(group);
        iNodes.add(bot, new PlainText("以下是你的背包↓:"));

        for (UserBackpack backpack : backpacks) {
            PropBase prop = PropsManager.getProp(backpack);
            if (prop == null) {
                continue;
            }
            iNodes.add(bot, new PlainText(String.format("物品id:%d%n%s", backpack.getPropId(), prop)));
        }
        iNodes.add(bot, MessageUtil.formatMessage("--- 当前页数: %d / 最大页数: %d ---", currentPage, maxPage));
        group.sendMessage(iNodes.build());
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    @MessageAuthorize(
            text = "use( \\d+)+|使用( \\d+)+",
            messageMatching = MessageMatchingEnum.REGULAR
    )
    public void useProp(GroupMessageEvent event) {
        Member sender = event.getSender();
        MessageChain message = event.getMessage();
        String content = message.contentToString();
        Group group = event.getSubject();

        UserInfo userInfo = UserManager.getUserInfo(sender);
        UseEvent useEvent = new UseEvent(sender, group, userInfo);


        String[] split = content.split(" ");

        MessageChainBuilder builder = new MessageChainBuilder();
        builder.add(new QuoteReply(message));
        builder.add("本次使用道具:");

        for (int i = 1; i < split.length; i++) {
            long propId = Long.parseLong(split[i]);


            List<UserBackpack> backpacks = userInfo.getBackpacks();

            boolean success = false;
            for (Iterator<UserBackpack> iterator = backpacks.iterator(); iterator.hasNext(); ) {
                UserBackpack backpack = iterator.next();
                if (backpack.getPropId().equals(propId)) {
                    PropBase prop = PropsManager.getProp(backpack);
                    String messageProp = "使用成功";
                    boolean remove = false;
                    try {
                        prop.use(useEvent);
                    } catch (Exception e) {
                        if (e instanceof Operation) {
                            Operation operation = (Operation) e;
                            remove = operation.isRemove();
                            messageProp = operation.getMessage();
                        } else {
                            throw e;
                        }
                    }
                    if (remove) {
                        delPropToBackpack(userInfo, backpack);
                    } else {
                        PropsManager.updateProp(backpack.getPropId(), prop);
                    }
                    builder.add(MessageUtil.formatMessage("\n%d %s!", propId, messageProp));
                    success = true;
                    break;
                }
            }
            if (!success) {
                builder.add(MessageUtil.formatMessage("\n%d 你没有这个道具!", propId));
            }
        }

        group.sendMessage(builder.build());
    }


    @MessageAuthorize(
            text = "dis( \\d+)+|丢弃( \\d+)+",
            messageMatching = MessageMatchingEnum.REGULAR
    )
    public void discard(GroupMessageEvent event) {
        Member sender = event.getSender();
        MessageChain message = event.getMessage();
        String content = message.contentToString();
        Group group = event.getSubject();

        String[] split = content.split(" ");

        MessageChainBuilder builder = new MessageChainBuilder();
        builder.add(new QuoteReply(message));
        builder.add("本次丢弃道具:");

        UserInfo userInfo = UserManager.getUserInfo(sender);

        for (int i = 1; i < split.length; i++) {
            long propId = Long.parseLong(split[i]);

            List<UserBackpack> backpacks = userInfo.getBackpacks();
            boolean match = false;
            for (UserBackpack backpack : backpacks) {
                if (backpack.getPropId().equals(propId)) {
                    PropBase prop = PropsManager.getProp(backpack);
                    String name = prop.getName();
                    delPropToBackpack(userInfo, propId);
                    builder.add(MessageUtil.formatMessage("\n你丢掉了你的 %s 。", name));
                    match = true;
                    break;
                }
            }

            if (!match) {
                builder.add(MessageUtil.formatMessage("\n没找到 %d 的道具。", propId));
            }
        }

        group.sendMessage(builder.build());
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

    /**
     * 给这个用户删除这个道具
     *
     * @param userInfo 用户
     * @param id       道具id
     */
    public static void delPropToBackpack(UserInfo userInfo, Long id) {
        List<UserBackpack> backpacks = userInfo.getBackpacks();

        UserBackpack find = null;
        for (UserBackpack backpack : backpacks) {
            if (backpack.getPropId().equals(id)) {
                find = backpack;
                break;
            }
        }

        userInfo.removePropInBackpack(find);

        PropsManager.destroyPros(id);
    }

    /**
     * 给这个用户删除这个道具
     *
     * @param userInfo     用户
     * @param userBackpack 道具
     */
    public static void delPropToBackpack(UserInfo userInfo, UserBackpack userBackpack) {
        userInfo.removePropInBackpack(userBackpack);
        PropsManager.destroyPros(userBackpack.getPropId());
    }

    /**
     * 检查该用户是否拥有这个道具
     *
     * @param userInfo 用户信息
     * @param id       道具id
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

    /**
     * 检查该用户是否拥有这个道具
     *
     * @param userInfo 用户信息
     * @param code     道具code
     * @return true 拥有
     */
    public static boolean checkPropInUser(UserInfo userInfo, String code) {
        for (UserBackpack backpack : userInfo.getBackpacks()) {
            if (Objects.equals(backpack.getPropCode(), code)) {
                return true;
            }
        }
        return false;
    }

}
