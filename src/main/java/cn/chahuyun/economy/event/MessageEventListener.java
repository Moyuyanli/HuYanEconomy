package cn.chahuyun.economy.event;

import cn.chahuyun.config.EconomyConfig;
import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.manager.*;
import cn.chahuyun.economy.plugin.PluginManager;
import cn.chahuyun.economy.utils.Log;
import cn.chahuyun.economy.utils.MessageUtil;
import kotlin.coroutines.CoroutineContext;
import net.mamoe.mirai.contact.*;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.EventCancelledException;
import net.mamoe.mirai.event.events.MessageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * 说明
 *
 * @author Moyuyanli
 * @Description :消息检测
 * @Date 2022/7/9 18:11
 */
public class MessageEventListener extends SimpleListenerHost {


    @Override
    public void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception) {
        if (exception instanceof EventCancelledException) {
            Log.error("发送消息被取消:", exception);
        } else if (exception instanceof BotIsBeingMutedException) {
            Log.error("你的机器人被禁言:", exception);
        } else if (exception instanceof MessageTooLargeException) {
            Log.error("发送消息过长:", exception);
        } else if (exception instanceof IllegalArgumentException) {
            Log.error("发送消息为空:", exception);
        }

        // 处理事件处理时抛出的异常
        Log.error(exception);
    }

    /**
     * 消息入口
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/11/14 12:34
     */
    @EventHandler()
    public void onMessage(@NotNull MessageEvent event) {
        EconomyConfig config = HuYanEconomy.INSTANCE.config;
        User sender = event.getSender();
        //主人
        boolean owner = config.getOwner() == sender.getId();
        Contact subject = event.getSubject();
        Group group = null;
        if (subject instanceof Group) {
            group = (Group) subject;
        }

        String code = event.getMessage().serializeToMiraiCode();
        PropsManager propsManager = PluginManager.getPropsManager();

        switch (code) {
            case "测试":
                return;
            case "签到":
            case "打卡":
            case "sign":
                Log.info("签到指令");
                SignManager.sign(event);
                return;
            case "个人信息":
            case "info":
                Log.info("个人信息指令");
                UserManager.getUserInfoImage(event);
                return;
            case "背包":
            case "backpack":
                Log.info("背包指令");
                propsManager.viewUserBackpack(event);
                return;
            case "道具商店":
            case "shops":
                Log.info("道具商店指令");
                propsManager.propStore(event);
                return;
            case "开启 猜签":
                if (owner) {
                    Log.info("管理指令");
                    if (group != null && !config.getLotteryGroup().contains(group.getId())) {
                        EconomyConfig.INSTANCE.getLotteryGroup().add(group.getId());
                    }
                    subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"本群的猜签功能已开启!"));
                }
                return;
            case "关闭 猜签":
                if (owner) {
                    Log.info("管理指令");
                    if (group != null && config.getLotteryGroup().contains(group.getId())) {
                        EconomyConfig.INSTANCE.getLotteryGroup().remove(group.getId());
                    }
                    subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"本群的猜签功能已关闭!"));
                }
                return;
            case "开启 钓鱼":
                if (owner) {
                    Log.info("管理指令");
                    if (group != null && !config.getFishGroup().contains(group.getId())) {
                        EconomyConfig.INSTANCE.getFishGroup().add(group.getId());
                    }
                    subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"本群的钓鱼功能已开启!"));
                }
                return;
            case "关闭 钓鱼":
                if (owner) {
                    Log.info("管理指令");
                    if (group != null && config.getFishGroup().contains(group.getId())) {
                        EconomyConfig.INSTANCE.getFishGroup().remove(group.getId());
                    }
                    subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"本群的钓鱼功能已关闭!"));
                }
                return;
            case "购买鱼竿":
                Log.info("游戏指令");
                GamesManager.buyFishRod(event);
                return;
            case "钓鱼":
            case "抛竿":
                Log.info("游戏指令");
                if (group != null && config.getFishGroup().contains(group.getId())) {
                    GamesManager.fishing(event);
                }
                return;
            case "升级鱼竿":
                Log.info("游戏指令");
                GamesManager.upFishRod(event);
                return;
            case "钓鱼排行榜":
            case "钓鱼排行":
            case "钓鱼榜":
                Log.info("游戏指令");
                GamesManager.fishTop(event);
                return;
            case "鱼竿等级":
                Log.info("游戏指令");
                GamesManager.viewFishLevel(event);
                return;
            case "刷新钓鱼":
                if (owner) {
                    Log.info("游戏指令");
                    GamesManager.refresh(event);
                }
                return;
            case "银行利率":
                Log.info("银行指令");
                BankManager.viewBankInterest(event);
                return;

        }

        String buyPropRegex = "购买 (\\S+)( \\S+)?|buy (\\S+)( \\S+)?";
        if (Pattern.matches(buyPropRegex, code)) {
            Log.info("购买指令");
            propsManager.buyPropFromStore(event);
            return;
        }

        String userPropRegex = "使用 (\\S+)( \\S+)?|use (\\S+)( \\S+)?";
        if (Pattern.matches(userPropRegex, code)) {
            Log.info("使用指令");
            propsManager.userProp(event);
            return;
        }

        String buyLotteryRegex = "猜签 (\\d+)( \\d+)|lottery (\\d+)( \\d+)";
        if (Pattern.matches(buyLotteryRegex, code)) {
            Log.info("彩票指令");
            if (group != null && config.getLotteryGroup().contains(group.getId())) {
                LotteryManager.addLottery(event);
            }
            return;
        }

        String userToUserTransferRegex = "转账(\\[mirai:at:\\d+])? \\d+( \\d+)?";
        if (Pattern.matches(userToUserTransferRegex, code)) {
            Log.info("转账指令");
            TransferManager.userToUser(event);
            return;
        }

        String walletToBankRegex = "存款 \\d+|deposit \\d+";
        String bankToWalletRegex = "取款 \\d+|withdraw \\d+";
        if (Pattern.matches(walletToBankRegex, code)) {
            Log.info("银行指令");
            BankManager.deposit(event);
            return;
        } else if (Pattern.matches(bankToWalletRegex, code)) {
            Log.info("银行指令");
            BankManager.withdrawal(event);
            return;
        }


//        {
//            if (group == null) {
//                return;
//            }
//            String regex = "转账\\s+(@?\\d+)\\s+(\\d+)";
//            //  String s = "转账    2482065472    12";
//            Matcher matcher = Pattern.compile(regex).matcher(event.getMessage().contentToString());
//            System.out.println(event.getMessage().contentToString());
//            MessageChainBuilder messages = new MessageChainBuilder();
//            if (matcher.matches()) {
//                int money = Integer.parseInt(matcher.group(2));
//                long toId = Long.parseLong(matcher.group(1).replaceAll("@", ""));
//                messages.append(TransferManager.transfer(event.getSender(), group.get(toId), money));
//                event.getSubject().sendMessage(messages.build());
//            }
//            return;
//        }

    }

}
